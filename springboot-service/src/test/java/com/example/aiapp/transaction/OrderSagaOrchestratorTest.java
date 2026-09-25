package com.example.aiapp.transaction;

import com.example.aiapp.transaction.dao.CustomerAccountDao;
import com.example.aiapp.transaction.dao.InventoryItemDao;
import com.example.aiapp.transaction.dao.OrderDao;
import com.example.aiapp.transaction.dao.OrderLineItemDao;
import com.example.aiapp.transaction.dao.PaymentDao;
import com.example.aiapp.transaction.dao.ShipmentDao;
import com.example.aiapp.transaction.dao.StockReservationDao;
import com.example.aiapp.transaction.entity.CustomerAccount;
import com.example.aiapp.transaction.entity.InventoryItem;
import com.example.aiapp.transaction.entity.OrderEntity;
import com.example.aiapp.transaction.entity.OrderStatus;
import com.example.aiapp.transaction.entity.PaymentEntity;
import com.example.aiapp.transaction.entity.PaymentStatus;
import com.example.aiapp.transaction.entity.ShipmentEntity;
import com.example.aiapp.transaction.entity.ShipmentStatus;
import com.example.aiapp.transaction.exception.DuplicateOrderException;
import com.example.aiapp.transaction.exception.InsufficientStockException;
import com.example.aiapp.transaction.exception.PaymentDeclinedException;
import com.example.aiapp.transaction.exception.ShippingUnavailableException;
import com.example.aiapp.transaction.model.PlaceOrderCommand;
import com.example.aiapp.transaction.saga.OrderSagaOrchestrator;
import com.example.aiapp.transaction.service.InventoryService;
import com.example.aiapp.transaction.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end tests for the place-order saga against a real (in-memory) database.
 *
 * <p><b>Why there is no {@code @Transactional} on this class.</b> The usual
 * rollback-after-each-test trick is actively wrong here: every participant method is
 * {@code REQUIRES_NEW}, so it suspends the test transaction and commits independently. The
 * rows would survive the rollback anyway, and worse, the test would be asserting against a
 * transaction that does not see them. So the tests commit for real and clean up explicitly
 * in {@link #resetDatabase()} - which is also a closer match to how this code runs in
 * production.</p>
 *
 * <p>{@code OrderSagaOrchestrator.placeOrder()} is {@code void} and simply rethrows
 * whatever the failing step threw (after best-effort compensation) - there is no wrapper
 * exception and no persisted saga log, so these tests assert against the actual entity
 * state (orders, payments, stock, shipments) rather than a saga trace.</p>
 */
@SpringBootTest(classes = SagaTestConfig.class)
@ActiveProfiles("test")
class OrderSagaOrchestratorTest {

    private static final String SKU = "SKU-WIDGET";
    private static final String SKU_SCARCE = "SKU-SCARCE";
    private static final Long CUSTOMER = 1001L;
    private static final Long BROKE_CUSTOMER = 1002L;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("100.00");
    private static final int INITIAL_STOCK = 10;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("5000.00");

    @Autowired
    private OrderSagaOrchestrator orchestrator;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private OrderLineItemDao orderLineItemDao;
    @Autowired
    private InventoryItemDao inventoryItemDao;
    @Autowired
    private StockReservationDao stockReservationDao;
    @Autowired
    private CustomerAccountDao customerAccountDao;
    @Autowired
    private PaymentDao paymentDao;
    @Autowired
    private ShipmentDao shipmentDao;

    @BeforeEach
    void resetDatabase() {
        orderLineItemDao.deleteAll();
        orderDao.deleteAll();
        stockReservationDao.deleteAll();
        inventoryItemDao.deleteAll();
        paymentDao.deleteAll();
        customerAccountDao.deleteAll();
        shipmentDao.deleteAll();

        inventoryItemDao.save(InventoryItem.builder()
                .sku(SKU).description("Widget").availableQty(INITIAL_STOCK).reservedQty(0).build());
        inventoryItemDao.save(InventoryItem.builder()
                .sku(SKU_SCARCE).description("Scarce").availableQty(1).reservedQty(0).build());

        customerAccountDao.save(CustomerAccount.builder()
                .customerId(CUSTOMER).fullName("Solvent Customer")
                .balance(INITIAL_BALANCE).currency("INR").build());
        customerAccountDao.save(CustomerAccount.builder()
                .customerId(BROKE_CUSTOMER).fullName("Broke Customer")
                .balance(new BigDecimal("10.00")).currency("INR").build());
    }

    // ------------------------------------------------------------------ happy path

    @Test
    @DisplayName("happy path: all five steps commit and the order ends CONFIRMED")
    void happyPath() {
        orchestrator.placeOrder(command("ORD-OK", SKU, 2, CUSTOMER, "IN"));

        OrderEntity order = order("ORD-OK");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("200.00");

        InventoryItem stock = inventoryItemDao.findBySku(SKU).orElseThrow();
        assertThat(stock.getAvailableQty()).isEqualTo(INITIAL_STOCK - 2);
        assertThat(stock.getReservedQty()).isEqualTo(2);

        assertThat(balanceOf(CUSTOMER)).isEqualByComparingTo("4800.00");

        PaymentEntity payment = paymentDao.findByOrderRef("ORD-OK").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);

        ShipmentEntity shipment = shipmentDao.findByOrderRef("ORD-OK").orElseThrow();
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.SCHEDULED);
    }

    // ------------------------------------------------------- failure at step 3 of 5

    @Test
    @DisplayName("out of stock: fails at reserveStock after payment already succeeded, so the charge is refunded")
    void outOfStock() {
        PlaceOrderCommand cmd = command("ORD-NOSTOCK", SKU_SCARCE, 50, CUSTOMER, "IN");

        assertThatThrownBy(() -> orchestrator.placeOrder(cmd))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(order("ORD-NOSTOCK").getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // The conditional UPDATE matched nothing, so stock is untouched and no reservation
        // row exists - there was never anything to release.
        InventoryItem stock = inventoryItemDao.findBySku(SKU_SCARCE).orElseThrow();
        assertThat(stock.getAvailableQty()).isEqualTo(1);
        assertThat(stock.getReservedQty()).isZero();
        assertThat(stockReservationDao.count()).isZero();

        // charge() runs before reserveStock(), so by the time stock is found to be short the
        // customer has already been charged - compensation refunds it, netting back to the
        // starting balance, and the payment row is left REFUNDED rather than absent.
        assertThat(balanceOf(CUSTOMER)).isEqualByComparingTo(INITIAL_BALANCE);
        PaymentEntity payment = paymentDao.findByOrderRef("ORD-NOSTOCK").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(shipmentDao.count()).isZero();
    }

    // ------------------------------------------------------- failure at step 2 of 5

    @Test
    @DisplayName("payment declined: fails at charge() before inventory is ever touched")
    void paymentDeclined() {
        PlaceOrderCommand cmd = command("ORD-POOR", SKU, 1, BROKE_CUSTOMER, "IN");

        assertThatThrownBy(() -> orchestrator.placeOrder(cmd))
                .isInstanceOf(PaymentDeclinedException.class);

        assertThat(order("ORD-POOR").getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // charge() runs before reserveStock(), so a declined payment means inventory is
        // never reached at all - nothing to release, no reservation ever created.
        InventoryItem stock = inventoryItemDao.findBySku(SKU).orElseThrow();
        assertThat(stock.getAvailableQty()).isEqualTo(INITIAL_STOCK);
        assertThat(stock.getReservedQty()).isZero();
        assertThat(stockReservationDao.count()).isZero();

        // The debit was refused outright, so the balance never moved and no row exists.
        assertThat(balanceOf(BROKE_CUSTOMER)).isEqualByComparingTo("10.00");
        assertThat(paymentDao.count()).isZero();
        assertThat(shipmentDao.count()).isZero();
    }

    // ------------------------------------------------------- failure at step 4 of 5

    @Test
    @DisplayName("shipping unavailable: fails at scheduleShipment and unwinds all three prior steps")
    void shippingUnavailableUnwindsEverything() {
        PlaceOrderCommand cmd = command("ORD-BR", SKU, 3, CUSTOMER, "BR");

        assertThatThrownBy(() -> orchestrator.placeOrder(cmd))
                .isInstanceOf(ShippingUnavailableException.class);

        // This is the interesting assertion: money was taken and given back.
        PaymentEntity payment = paymentDao.findByOrderRef("ORD-BR").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isNotNull();
        assertThat(balanceOf(CUSTOMER)).isEqualByComparingTo(INITIAL_BALANCE);

        InventoryItem stock = inventoryItemDao.findBySku(SKU).orElseThrow();
        assertThat(stock.getAvailableQty()).isEqualTo(INITIAL_STOCK);
        assertThat(stock.getReservedQty()).isZero();

        assertThat(order("ORD-BR").getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(shipmentDao.count()).isZero();
    }

    // ------------------------------------------------------------------ idempotency

    @Test
    @DisplayName("compensators are idempotent: running them twice does not double-refund or double-release")
    void compensatorsAreIdempotent() {
        orchestrator.placeOrder(command("ORD-IDEM", SKU, 2, CUSTOMER, "IN"));
        assertThat(order("ORD-IDEM").getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        Long reservationId = stockReservationDao.findAll().get(0).getId();
        Long paymentId = paymentDao.findByOrderRef("ORD-IDEM").orElseThrow().getId();

        // First undo - the real one.
        inventoryService.releaseStock(reservationId);
        paymentService.refund(paymentId);

        BigDecimal balanceAfterFirstRefund = balanceOf(CUSTOMER);
        int availableAfterFirstRelease = inventoryItemDao.findBySku(SKU).orElseThrow().getAvailableQty();
        assertThat(balanceAfterFirstRefund).isEqualByComparingTo(INITIAL_BALANCE);
        assertThat(availableAfterFirstRelease).isEqualTo(INITIAL_STOCK);

        // Second undo - a replay. Must be a silent no-op, not a second credit.
        inventoryService.releaseStock(reservationId);
        paymentService.refund(paymentId);

        assertThat(balanceOf(CUSTOMER)).isEqualByComparingTo(balanceAfterFirstRefund);
        assertThat(inventoryItemDao.findBySku(SKU).orElseThrow().getAvailableQty())
                .isEqualTo(availableAfterFirstRelease);
        assertThat(inventoryItemDao.findBySku(SKU).orElseThrow().getReservedQty()).isZero();
    }

    @Test
    @DisplayName("a duplicate orderRef is rejected at the first step, so a replay cannot charge twice")
    void duplicateOrderRefIsRejected() {
        orchestrator.placeOrder(command("ORD-DUP", SKU, 1, CUSTOMER, "IN"));
        BigDecimal balanceAfterFirst = balanceOf(CUSTOMER);

        PlaceOrderCommand duplicate = command("ORD-DUP", SKU, 1, CUSTOMER, "IN");
        assertThatThrownBy(() -> orchestrator.placeOrder(duplicate))
                .isInstanceOf(DuplicateOrderException.class);

        assertThat(balanceOf(CUSTOMER)).isEqualByComparingTo(balanceAfterFirst);
        assertThat(paymentDao.count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ concurrency

    @Test
    @DisplayName("two sagas racing for the last unit cannot both win - no overselling")
    void concurrentSagasCannotOversell() throws Exception {
        // SKU_SCARCE has exactly one unit.
        CyclicBarrier startLine = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> attempt = () -> {
                startLine.await(5, TimeUnit.SECONDS);
                try {
                    orchestrator.placeOrder(command(
                            "ORD-RACE-" + Thread.currentThread().threadId(),
                            SKU_SCARCE, 1, CUSTOMER, "IN"));
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            };

            Future<Boolean> first = pool.submit(attempt);
            Future<Boolean> second = pool.submit(attempt);
            long winners = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))
                    .stream().filter(Boolean::booleanValue).count();

            // At most one can win. (It may be zero if the database aborts both on lock
            // contention - the invariant being protected is that it is never two.)
            assertThat(winners).isLessThanOrEqualTo(1L);

            InventoryItem stock = inventoryItemDao.findBySku(SKU_SCARCE).orElseThrow();
            assertThat(stock.getAvailableQty()).isNotNegative();
            assertThat(stock.getAvailableQty() + stock.getReservedQty()).isEqualTo(1);
            assertThat(stock.getReservedQty()).isEqualTo((int) winners);
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------------- helpers

    private static PlaceOrderCommand command(String ref, String sku, int qty, Long customerId, String country) {
        return new PlaceOrderCommand(ref, customerId, sku, qty, UNIT_PRICE, country, "1 Test Street");
    }

    private OrderEntity order(String orderRef) {
        return orderDao.findByOrderRef(orderRef).orElseThrow(
                () -> new AssertionError("No order with ref " + orderRef));
    }

    private BigDecimal balanceOf(Long customerId) {
        return customerAccountDao.findByCustomerId(customerId).orElseThrow().getBalance();
    }
}
