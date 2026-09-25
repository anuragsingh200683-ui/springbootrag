# Saga Orchestration — `com.example.aiapp.transaction`

A minimal, readable implementation of the **orchestration-based Saga pattern**: one
coordinator driving four participant services, with best-effort compensating actions that
undo committed work when a later step fails.

Only two layers, as intended: **service** (business logic) and **dao** (data access).
Entities and small value objects support those.

---

## 1. The problem

Placing an order touches four independently-owned pieces of state:

| Owner | State |
|---|---|
| `OrderService` | the order and its line items |
| `PaymentService` | customer wallet and payments |
| `InventoryService` | stock levels and reservations |
| `ShippingService` | carrier bookings |

You cannot wrap these in one `@Transactional` once they live in separate services —
there is no shared connection to roll back. Distributed two-phase commit (XA) *can* do it,
but it holds locks across every participant for the whole duration and turns any single
slow or dead participant into a system-wide stall. In practice nobody runs it.

## 2. What a saga is

> A saga is a sequence of local transactions. Each one commits on its own, and each has a
> **compensating action** that semantically undoes it. If a step fails, the coordinator
> runs the compensators for every step that already succeeded, in reverse.

"Semantically undoes" is the important phrase. The original transaction has already
committed and is visible to everyone. You cannot roll it back — you issue a *new* business
fact that negates it: a cancellation, a refund, a stock release.

## 3. Orchestration vs choreography

There are two ways to sequence the steps.

**Choreography** — no coordinator. Each service publishes an event; the next one reacts.
More decoupled, but no single place knows the order of steps, and answering *"where did
order X get stuck?"* means correlating four services' logs.

**Orchestration** — a coordinator calls each service directly and handles its failure.
**This is what this package implements**, in `OrderSagaOrchestrator.placeOrder()`.

```
                    ┌─────────────────────────┐
                    │  OrderSagaOrchestrator  │
                    └────────────┬────────────┘
           ┌─────────────┬───────┴───────┬──────────────┐
           ▼             ▼               ▼              ▼
    OrderService  PaymentService   InventoryService  ShippingService
```

The whole sequence and its undo live in one method you can read top to bottom - no
framework, no generic step engine, just five calls in a `try` block and one `compensate()`
method underneath.

## 4. The steps

| # | Step | Forward action (commits) | Compensating action |
|---|------|--------------------------|---------------------|
| 1 | create order | insert order + line items, `PENDING` | set status `CANCELLED` |
| 2 | charge | atomically debit wallet, insert `CAPTURED` payment | credit wallet, payment `REFUNDED` |
| 3 | reserve stock | atomically move qty `available → reserved`, insert reservation | move it back, reservation `RELEASED` |
| 4 | schedule shipment | insert shipment `SCHEDULED` | shipment `CANCELLED` |
| 5 | confirm order | order → `CONFIRMED` — **the pivot** | *(nothing after it can fail)* |

**Payment before inventory, deliberately.** Charging runs before reserving stock. The
trade-off: a customer who cannot pay is turned away before any stock is touched, but a
customer who pays for something that turns out to be out of stock has their payment
captured and then immediately refunded rather than never charged at all. See the
`outOfStock` test, which exercises exactly that refund path.

### Happy path

```
createOrder ──▶ charge ──▶ reserveStock ──▶ scheduleShipment ──▶ confirmOrder
     ✓             ✓             ✓                  ✓                 ✓
```

### Failure at step 4 (shipping unavailable)

```
createOrder ──▶ charge ──▶ reserveStock ──▶ scheduleShipment
     ✓             ✓             ✓                  ✗  ShippingUnavailableException
     │             │             │                  │
     ▼             ▼             ▼                  ▼
  cancelOrder ◀── refund ◀── releaseStock ◀── cancelShipment (no-op: nothing was created)
```

## 5. The actual code

```java
public void placeOrder(PlaceOrderCommand command) {
    UUID sagaId = UUID.randomUUID();

    Long orderId = null;
    Long paymentId = null;
    Long reservationId = null;
    Long shipmentId = null;

    try {
        orderId = orderService.createOrder(sagaId, command);
        paymentId = paymentService.charge(sagaId, command.orderRef(), command.customerId(), command.totalAmount());
        reservationId = inventoryService.reserveStock(sagaId, command.orderRef(), command.sku(), command.quantity());
        shipmentId = shippingService.scheduleShipment(sagaId, command.orderRef(), command.destinationCountry(), command.shippingAddress());
        orderService.confirmOrder(orderId);

    } catch (Exception e) {
        compensate(orderId, paymentId, reservationId, shipmentId, e);
        throw e;
    }
}
```

`compensate()` unconditionally calls all four undo actions (cancel shipment, release stock,
refund, cancel order), each wrapped in its own `try/catch` so one broken compensator does
not stop the rest. This is safe *only* because of the idempotency guarantee below - a
compensator whose forward step never ran (its id is still `null`, because the failure
happened before that assignment) is a safe no-op, not an error.

## 6. The transaction-boundary rule

**`placeOrder()` is deliberately NOT `@Transactional`.** Every participant method it calls
(`createOrder`, `charge`, `reserveStock`, `scheduleShipment`, `confirmOrder`) is
`@Transactional(propagation = Propagation.REQUIRES_NEW)` and commits on its own. If the
orchestrator held an outer transaction, a failure would roll every step back together, and
the compensating calls below would be "undoing" work the database had already discarded.
`REQUIRES_NEW` also defends against a *caller* that holds its own transaction, for the same
reason.

The same reasoning is documented on `com.example.aiapp.service.DocumentService`, which is
non-transactional for exactly this reason.

## 7. Idempotency — why `compensate()` can be unconditional

Every compensator re-reads current state and returns quietly if the undo already happened
(or never needed to happen):

```java
// OrderServiceImpl.cancelOrder
if (orderId == null) {
    return;
}
```

Each of `cancelOrder`, `refund`, `releaseStock`, and `cancelShipment` has this same
null-and-status guard. That is what lets `compensate()` in `OrderSagaOrchestrator` call all
four unconditionally, regardless of how far the saga actually got, instead of needing to
track which steps ran.

`orders.order_ref` being unique is the saga-level version of the same idea: a replayed
`placeOrder()` call with the same `orderRef` fails at step 1, before any money moves.

## 8. What a saga gives up

**Isolation — the I in ACID.** Between charging and a later compensating refund, another
reader genuinely sees the debited balance. There is no way around this; it is the price of
spanning services at all.

**Semantic lock.** `reserveStock` does not lock the inventory row for the saga's duration.
It moves quantity into a `reserved_qty` bucket nobody else can sell. The lock is expressed
in the data model rather than the transaction manager - see the conditional `UPDATE` below.

## 9. Concurrency

Stock and balance changes are **conditional bulk UPDATEs**, not read-modify-write:

```java
@Query("""
       update InventoryItem i
          set i.availableQty = i.availableQty - :qty, ...
        where i.sku = :sku
          and i.availableQty >= :qty
       """)
int tryReserve(...);   // returns 0 when there was not enough
```

Two concurrent sagas reading `available_qty = 1` would both conclude the unit was theirs.
A single conditional UPDATE is evaluated by the database under a row lock, so exactly one
of them affects a row — no optimistic-lock retry loop, no `@Version` column.
`concurrentSagasCannotOversell` races two sagas for one unit and asserts the invariant.

## 10. Layout

```
transaction/
├── saga/       OrderSagaOrchestrator  (the whole coordinator - one file)
├── service/    Order/Payment/Inventory/Shipping  (interface + Impl)
├── dao/        *Dao interfaces extending JpaRepository
├── entity/     JPA entities + status enums
├── model/      PlaceOrderCommand
├── exception/  business exceptions (InsufficientStockException, PaymentDeclinedException, ...)
├── config/     TransactionProperties  (serviceable-countries, demo seed toggle)
└── demo/       SagaDemoRunner  (@Profile("saga-demo"))
```

The participant services take **plain domain arguments and return ids** — they know
nothing about sagas. `OrderSagaOrchestrator.placeOrder()` is the only place that calls them
in order and reacts to their failures, which keeps each service usable and unit-testable
entirely on its own.

## 11. Running it

**Tests** (H2 in-memory, no external services needed):

```bash
mvn test -Dtest=OrderSagaOrchestratorTest
```

**Demo** — one success and three different failures, logged to the console:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=saga-demo
```

There is no controller in this package - `OrderSagaOrchestrator.placeOrder()` is called
directly by the test suite and by `SagaDemoRunner`. Add a thin `@RestController` calling
`placeOrder()` if you need an HTTP entry point; nothing in the saga itself depends on one.

## 12. Deliberately out of scope

| Not implemented | How you would add it |
|---|---|
| **Audit trail / observability** | There is no persisted saga log - a failed compensation is only a log line (`// retry / recovery` in `compensate()`), not a durable record. Add a `saga_log` table and write to it from `compensate()`/`placeOrder()` if you need to query "what happened to saga X" after the fact. |
| **Retry** on transient failures | `spring-retry` + `@Retryable` on participant methods. Only safe because the steps are idempotent. |
| **Crash recovery** | Nothing currently detects a saga that died mid-flight (e.g. the JVM crashed between two steps) - that needs the audit trail above plus a recovery sweep. |
| **Async / event-driven** | Replace the direct calls with commands on a queue and reply events. Closer to a real distributed saga; considerably more machinery. |
| **HTTP entry point** | A thin `@RestController` delegating straight to `placeOrder()`. |

This synchronous, in-process version is intentionally the simplest correct implementation
of the pattern - every extension above is an addition, not a rewrite.
