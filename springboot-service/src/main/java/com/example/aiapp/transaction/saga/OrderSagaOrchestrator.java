package com.example.aiapp.transaction.saga;

import com.example.aiapp.transaction.config.TransactionProperties;
import com.example.aiapp.transaction.model.PlaceOrderCommand;
import com.example.aiapp.transaction.service.InventoryService;
import com.example.aiapp.transaction.service.OrderService;
import com.example.aiapp.transaction.service.PaymentService;
import com.example.aiapp.transaction.service.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.time.Duration;
import java.util.UUID;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final TransactionProperties.Compensation compensation;

    public OrderSagaOrchestrator(
            OrderService orderService,
            PaymentService paymentService,
            InventoryService inventoryService,
            ShippingService shippingService,
            TransactionProperties transactionProperties) {

        this.orderService = orderService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.shippingService = shippingService;
        this.compensation = transactionProperties.getCompensation();
    }

    public void placeOrder(PlaceOrderCommand command) {
        UUID sagaId = UUID.randomUUID();

        Long orderId = null;
        Long paymentId = null;
        Long reservationId = null;
        Long shipmentId = null;

        try {

            // Step 1
            orderId = orderService.createOrder(sagaId, command);

            // Step 2
            paymentId = paymentService.charge(
                    sagaId, command.orderRef(), command.customerId(), command.totalAmount());

            // Step 3
            reservationId = inventoryService.reserveStock(
                    sagaId, command.orderRef(), command.sku(), command.quantity());

            // Step 4
            shipmentId = shippingService.scheduleShipment(
                    sagaId, command.orderRef(), command.destinationCountry(), command.shippingAddress());

            // Step 5
            orderService.confirmOrder(orderId);

        } catch (Exception e) {

            compensate(sagaId, orderId, paymentId, reservationId, shipmentId, e);

            throw e;
        }
    }

    private void compensate(UUID sagaId, Long orderId, Long paymentId, Long reservationId, Long shipmentId,
                             Exception cause) {

        // One retry budget is shared by every compensating action in this run.
        long retryDeadline = System.nanoTime() + compensation.getRetryBudget().toNanos();

        retry(sagaId, "cancelShipment", shipmentId, cause, retryDeadline,
                "the shipment may still be scheduled",
                () -> shippingService.cancelShipment(shipmentId));

        retry(sagaId, "releaseStock", reservationId, cause, retryDeadline,
                "the stock stays reserved and unavailable for sale",
                () -> inventoryService.releaseStock(reservationId));

        retry(sagaId, "refund", paymentId, cause, retryDeadline,
                "the customer was charged and NOT refunded",
                () -> paymentService.refund(paymentId));

        retry(sagaId, "cancelOrder", orderId, cause, retryDeadline,
                "the order stays in an inconsistent status",
                () -> orderService.cancelOrder(orderId, cause.getMessage()));
    }

    /**
     * Runs one compensating action, retrying it only when the failure is transient (a dropped
     * connection, a lock or query timeout) and the run's retry budget still has time left.
     *
     * <p>Every action gets at least one attempt. Retries stop when the error is not transient,
     * the attempts run out, the retry budget is used up, or the thread is interrupted. An
     * interrupt cancels only the waiting: the remaining actions still get one attempt each,
     * because skipping a refund is worse than trying it. The interrupt flag stays set for the
     * caller.</p>
     *
     * <p>Never throws: if the action finally fails, the failure is logged once at ERROR (naming
     * the step, the id it was acting on, the original failure and the concrete impact) and
     * execution moves on to the next compensation. One broken compensator must not stop the rest
     * of the undo, and must never hide the original failure from the caller of
     * {@link #placeOrder}.</p>
     */
    private void retry(UUID sagaId, String stepName, Long targetId, Exception cause, long retryDeadline,
                       String impactIfFailed, Runnable action) {

        int maxAttempts = Math.max(1, compensation.getMaxAttempts());

        for (int attempt = 1; ; attempt++) {
            try {
                action.run();
                if (attempt > 1) {
                    log.warn("[saga={}] compensation {}(id={}) succeeded on attempt {}/{}",
                            sagaId, stepName, targetId, attempt, maxAttempts);
                }
                return;
            } catch (Exception e) {
                long backoffNanos = compensation.getBackoff().toNanos() * attempt;
                String stopReason = reasonToStopRetrying(e, attempt, maxAttempts, retryDeadline, backoffNanos);
                if (stopReason != null) {
                    logGaveUp(sagaId, stepName, targetId, attempt, stopReason, impactIfFailed, cause, e);
                    return;
                }

                log.warn("[saga={}] compensation attempt {}/{} failed: {}(id={}) - retrying. Error: {}",
                        sagaId, attempt, maxAttempts, stepName, targetId, e.getMessage());

                if (!sleep(backoffNanos)) {
                    logGaveUp(sagaId, stepName, targetId, attempt, "interrupted while waiting to retry",
                            impactIfFailed, cause, e);
                    return;
                }
            }
        }
    }

    /** @return why no further attempt should be made, or {@code null} if a retry is allowed */
    private static String reasonToStopRetrying(Exception e, int attempt, int maxAttempts, long retryDeadline,
                                               long backoffNanos) {
        if (!isTransient(e)) {
            return "error is not transient";
        }
        if (attempt >= maxAttempts) {
            return "no attempts left";
        }
        if (Thread.currentThread().isInterrupted()) {
            return "thread was interrupted";
        }
        if (System.nanoTime() + backoffNanos - retryDeadline > 0) {
            return "compensation retry budget used up";
        }
        return null;
    }

    /**
     * Whether a failure is worth retrying: a transient or recoverable database error, or a
     * failure to reach the database or start a transaction. Checks the whole cause chain, since
     * the JDBC exception is often wrapped. Anything else (bad state, bad arguments, a bug) would
     * fail the same way again.
     */
    static boolean isTransient(Throwable error) {
        Throwable t = error;
        while (t != null) {
            if (t instanceof TransientDataAccessException
                    || t instanceof RecoverableDataAccessException
                    || t instanceof DataAccessResourceFailureException
                    || t instanceof CannotCreateTransactionException
                    || t instanceof SQLTransientException
                    || t instanceof SQLRecoverableException) {
                return true;
            }
            t = (t.getCause() == t) ? null : t.getCause();
        }
        return false;
    }

    private static void logGaveUp(UUID sagaId, String stepName, Long targetId, int attempts, String stopReason,
                                  String impactIfFailed, Exception cause, Exception lastError) {
        log.error("[saga={}] compensation FAILED after {} attempt(s) ({}): {}(id={}) - {}, manual cleanup "
                        + "needed. Original failure: {}",
                sagaId, attempts, stopReason, stepName, targetId, impactIfFailed, cause.getMessage(), lastError);
    }

    /** @return true if the sleep completed normally, false if it was interrupted */
    private static boolean sleep(long nanos) {
        try {
            Thread.sleep(Duration.ofNanos(nanos));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
