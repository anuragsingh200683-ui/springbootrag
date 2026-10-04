package com.example.aiapp.transaction.saga;

import com.example.aiapp.transaction.config.TransactionProperties;
import com.example.aiapp.transaction.model.PlaceOrderCommand;
import com.example.aiapp.transaction.service.InventoryService;
import com.example.aiapp.transaction.service.OrderService;
import com.example.aiapp.transaction.service.PaymentService;
import com.example.aiapp.transaction.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.math.BigDecimal;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link OrderSagaOrchestrator}: all four participant services are
 * mocked, so these run with no Spring context and no database - just the orchestrator's own
 * control flow (call order, which ids get passed where, what gets compensated and in what
 * order, and that a broken compensator does not stop the rest).
 *
 * <p>See {@code OrderSagaOrchestratorTest} (in the parent package) for the complementary
 * end-to-end tests against real service implementations and an in-memory database.</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorUnitTest {

    private static final Long ORDER_ID = 1L;
    private static final Long PAYMENT_ID = 2L;
    private static final Long RESERVATION_ID = 3L;
    private static final Long SHIPMENT_ID = 4L;

    @Mock
    private OrderService orderService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private ShippingService shippingService;

    private OrderSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // Real retry logic, but 1ms waits so the tests don't actually sleep.
        orchestrator = orchestrator(3, Duration.ofMillis(1), Duration.ofSeconds(5));
    }

    private OrderSagaOrchestrator orchestrator(int maxAttempts, Duration backoff, Duration retryBudget) {
        TransactionProperties properties = new TransactionProperties();
        properties.getCompensation().setMaxAttempts(maxAttempts);
        properties.getCompensation().setBackoff(backoff);
        properties.getCompensation().setRetryBudget(retryBudget);
        return new OrderSagaOrchestrator(orderService, paymentService, inventoryService, shippingService, properties);
    }

    /** Runs steps 1-3 successfully and fails step 4, so all of stock, payment and order need undoing. */
    private RuntimeException failAtScheduleShipment() {
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenReturn(ORDER_ID);
        when(paymentService.charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class))).thenReturn(PAYMENT_ID);
        when(inventoryService.reserveStock(any(UUID.class), anyString(), anyString(), anyInt())).thenReturn(RESERVATION_ID);
        RuntimeException failure = new RuntimeException("carrier unavailable");
        when(shippingService.scheduleShipment(any(UUID.class), anyString(), anyString(), anyString())).thenThrow(failure);
        return failure;
    }

    private static RuntimeException transientFailure() {
        return new TransientDataAccessResourceException("connection reset");
    }

    private static PlaceOrderCommand command() {
        return new PlaceOrderCommand("ORD-1", 100L, "SKU-1", 2, new BigDecimal("50.00"), "IN", "1 Test Street");
    }

    @Test
    @DisplayName("happy path: all five steps run in order and nothing is compensated")
    void happyPath() {
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenReturn(ORDER_ID);
        when(paymentService.charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class))).thenReturn(PAYMENT_ID);
        when(inventoryService.reserveStock(any(UUID.class), anyString(), anyString(), anyInt())).thenReturn(RESERVATION_ID);
        when(shippingService.scheduleShipment(any(UUID.class), anyString(), anyString(), anyString())).thenReturn(SHIPMENT_ID);

        orchestrator.placeOrder(command());

        InOrder order = inOrder(orderService, paymentService, inventoryService, shippingService);
        order.verify(orderService).createOrder(any(UUID.class), any(PlaceOrderCommand.class));
        order.verify(paymentService).charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class));
        order.verify(inventoryService).reserveStock(any(UUID.class), anyString(), anyString(), anyInt());
        order.verify(shippingService).scheduleShipment(any(UUID.class), anyString(), anyString(), anyString());
        order.verify(orderService).confirmOrder(ORDER_ID);

        verify(orderService, never()).cancelOrder(any(), any());
        verify(paymentService, never()).refund(any());
        verify(inventoryService, never()).releaseStock(any());
        verify(shippingService, never()).cancelShipment(any());
    }

    @Test
    @DisplayName("fails at createOrder: nothing else is attempted, and every compensator still runs safely on null ids")
    void createOrderFails() {
        RuntimeException failure = new RuntimeException("db down");
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenThrow(failure);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(paymentService, never()).charge(any(), any(), any(), any());
        verify(inventoryService, never()).reserveStock(any(), any(), any(), anyInt());
        verify(shippingService, never()).scheduleShipment(any(), any(), any(), any());
        verify(orderService, never()).confirmOrder(any());

        verify(shippingService).cancelShipment(isNull());
        verify(inventoryService).releaseStock(isNull());
        verify(paymentService).refund(isNull());
        verify(orderService).cancelOrder(isNull(), org.mockito.ArgumentMatchers.eq("db down"));
    }

    @Test
    @DisplayName("fails at charge: the created order is cancelled, nothing further is attempted")
    void chargeFails() {
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenReturn(ORDER_ID);
        RuntimeException failure = new RuntimeException("card declined");
        when(paymentService.charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class))).thenThrow(failure);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(inventoryService, never()).reserveStock(any(), any(), any(), anyInt());
        verify(shippingService, never()).scheduleShipment(any(), any(), any(), any());
        verify(orderService, never()).confirmOrder(any());

        verify(shippingService).cancelShipment(isNull());
        verify(inventoryService).releaseStock(isNull());
        verify(paymentService).refund(isNull());
        verify(orderService).cancelOrder(ORDER_ID, "card declined");
    }

    @Test
    @DisplayName("fails at reserveStock: the payment already taken is refunded, the order cancelled")
    void reserveStockFails() {
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenReturn(ORDER_ID);
        when(paymentService.charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class))).thenReturn(PAYMENT_ID);
        RuntimeException failure = new RuntimeException("out of stock");
        when(inventoryService.reserveStock(any(UUID.class), anyString(), anyString(), anyInt())).thenThrow(failure);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(shippingService, never()).scheduleShipment(any(), any(), any(), any());
        verify(orderService, never()).confirmOrder(any());

        verify(shippingService).cancelShipment(isNull());
        verify(inventoryService).releaseStock(isNull());
        verify(paymentService).refund(PAYMENT_ID);
        verify(orderService).cancelOrder(ORDER_ID, "out of stock");
    }

    @Test
    @DisplayName("fails at scheduleShipment: stock released, payment refunded, order cancelled - in reverse order")
    void scheduleShipmentFailsAndUnwindsInReverseOrder() {
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenReturn(ORDER_ID);
        when(paymentService.charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class))).thenReturn(PAYMENT_ID);
        when(inventoryService.reserveStock(any(UUID.class), anyString(), anyString(), anyInt())).thenReturn(RESERVATION_ID);
        RuntimeException failure = new RuntimeException("carrier unavailable");
        when(shippingService.scheduleShipment(any(UUID.class), anyString(), anyString(), anyString())).thenThrow(failure);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(orderService, never()).confirmOrder(any());

        // Compensation is LIFO: shipment (never created, so a no-op) -> stock -> payment -> order.
        InOrder order = inOrder(shippingService, inventoryService, paymentService, orderService);
        order.verify(shippingService).cancelShipment(isNull());
        order.verify(inventoryService).releaseStock(RESERVATION_ID);
        order.verify(paymentService).refund(PAYMENT_ID);
        order.verify(orderService).cancelOrder(ORDER_ID, "carrier unavailable");
    }

    @Test
    @DisplayName("fails at confirmOrder: every prior step is fully compensated with its real id")
    void confirmOrderFails() {
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenReturn(ORDER_ID);
        when(paymentService.charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class))).thenReturn(PAYMENT_ID);
        when(inventoryService.reserveStock(any(UUID.class), anyString(), anyString(), anyInt())).thenReturn(RESERVATION_ID);
        when(shippingService.scheduleShipment(any(UUID.class), anyString(), anyString(), anyString())).thenReturn(SHIPMENT_ID);
        RuntimeException failure = new RuntimeException("invalid state transition");
        doThrow(failure).when(orderService).confirmOrder(ORDER_ID);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(shippingService).cancelShipment(SHIPMENT_ID);
        verify(inventoryService).releaseStock(RESERVATION_ID);
        verify(paymentService).refund(PAYMENT_ID);
        verify(orderService).cancelOrder(ORDER_ID, "invalid state transition");
    }

    @Test
    @DisplayName("a broken compensator does not stop the rest of the compensation chain")
    void brokenCompensatorDoesNotStopTheRest() {
        when(orderService.createOrder(any(UUID.class), any(PlaceOrderCommand.class))).thenReturn(ORDER_ID);
        when(paymentService.charge(any(UUID.class), anyString(), anyLong(), any(BigDecimal.class))).thenReturn(PAYMENT_ID);
        when(inventoryService.reserveStock(any(UUID.class), anyString(), anyString(), anyInt())).thenReturn(RESERVATION_ID);
        RuntimeException shippingFailure = new RuntimeException("carrier unavailable");
        when(shippingService.scheduleShipment(any(UUID.class), anyString(), anyString(), anyString())).thenThrow(shippingFailure);

        // The stock-release compensator itself blows up.
        doThrow(new RuntimeException("inventory service down")).when(inventoryService).releaseStock(RESERVATION_ID);

        // The caller still sees the ORIGINAL failure, not the compensator's.
        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(shippingFailure);

        // A non-transient error is not retried ...
        verify(inventoryService, times(1)).releaseStock(RESERVATION_ID);

        // ... and refund/cancelOrder still ran despite releaseStock blowing up.
        verify(paymentService).refund(PAYMENT_ID);
        verify(orderService).cancelOrder(ORDER_ID, "carrier unavailable");
    }

    @Test
    @DisplayName("a transient compensator failure is retried and succeeds on the second attempt")
    void transientFailureIsRetriedUntilItSucceeds() {
        RuntimeException failure = failAtScheduleShipment();
        doThrow(transientFailure()).doNothing().when(inventoryService).releaseStock(RESERVATION_ID);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(inventoryService, times(2)).releaseStock(RESERVATION_ID);
        verify(paymentService).refund(PAYMENT_ID);
        verify(orderService).cancelOrder(ORDER_ID, "carrier unavailable");
    }

    @Test
    @DisplayName("a compensator that keeps failing transiently gets maxAttempts tries, then the rest still run")
    void transientFailureGivesUpAfterMaxAttempts() {
        RuntimeException failure = failAtScheduleShipment();
        doThrow(transientFailure()).when(inventoryService).releaseStock(RESERVATION_ID);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(inventoryService, times(3)).releaseStock(RESERVATION_ID);
        verify(paymentService).refund(PAYMENT_ID);
        verify(orderService).cancelOrder(ORDER_ID, "carrier unavailable");
    }

    @Test
    @DisplayName("once the retry budget is used up, each compensator gets exactly one attempt")
    void usedUpRetryBudgetMeansOneAttemptEach() {
        orchestrator = orchestrator(3, Duration.ofMillis(1), Duration.ZERO);
        RuntimeException failure = failAtScheduleShipment();
        doThrow(transientFailure()).when(inventoryService).releaseStock(RESERVATION_ID);
        doThrow(transientFailure()).when(paymentService).refund(PAYMENT_ID);

        assertThatThrownBy(() -> orchestrator.placeOrder(command())).isSameAs(failure);

        verify(inventoryService, times(1)).releaseStock(RESERVATION_ID);
        verify(paymentService, times(1)).refund(PAYMENT_ID);
        verify(orderService).cancelOrder(ORDER_ID, "carrier unavailable");
    }

    @Test
    @DisplayName("an interrupt during the retry wait stops all retries, but every remaining compensator still runs once")
    void interruptDuringBackoffStopsRetriesButNotTheUndo() throws Exception {
        // Long waits: if the interrupt were ignored, the worker would still be sleeping when we check.
        orchestrator = orchestrator(3, Duration.ofSeconds(30), Duration.ofMinutes(5));
        RuntimeException failure = failAtScheduleShipment();

        CountDownLatch firstAttemptFailed = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstAttemptFailed.countDown();
            throw transientFailure();
        }).when(inventoryService).releaseStock(RESERVATION_ID);
        doThrow(transientFailure()).when(paymentService).refund(PAYMENT_ID);

        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean interruptFlagKept = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            try {
                orchestrator.placeOrder(command());
            } catch (Throwable t) {
                thrown.set(t);
            }
            interruptFlagKept.set(Thread.currentThread().isInterrupted());
        });

        worker.start();
        assertThat(firstAttemptFailed.await(5, TimeUnit.SECONDS)).isTrue();
        worker.interrupt();
        worker.join(5_000);

        assertThat(worker.isAlive()).as("worker should not be stuck in a backoff sleep").isFalse();
        assertThat(thrown.get()).isSameAs(failure);
        assertThat(interruptFlagKept.get()).as("interrupt flag is preserved for the caller").isTrue();

        verify(inventoryService, times(1)).releaseStock(RESERVATION_ID);
        verify(paymentService, times(1)).refund(PAYMENT_ID);
        verify(orderService).cancelOrder(ORDER_ID, "carrier unavailable");
    }

    @Test
    @DisplayName("only transient database and connection errors count as retryable, including when wrapped")
    void transientClassification() {
        assertThat(OrderSagaOrchestrator.isTransient(transientFailure())).isTrue();
        assertThat(OrderSagaOrchestrator.isTransient(new CannotCreateTransactionException("no connection"))).isTrue();
        assertThat(OrderSagaOrchestrator.isTransient(
                new RuntimeException("wrapped", new SQLTransientConnectionException("timeout")))).isTrue();

        assertThat(OrderSagaOrchestrator.isTransient(new IllegalStateException("bad status"))).isFalse();
        assertThat(OrderSagaOrchestrator.isTransient(new RuntimeException("inventory service down"))).isFalse();
    }
}
