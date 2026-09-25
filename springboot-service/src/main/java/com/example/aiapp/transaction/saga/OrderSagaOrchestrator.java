package com.example.aiapp.transaction.saga;

import com.example.aiapp.transaction.model.PlaceOrderCommand;
import com.example.aiapp.transaction.service.InventoryService;
import com.example.aiapp.transaction.service.OrderService;
import com.example.aiapp.transaction.service.PaymentService;
import com.example.aiapp.transaction.service.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    /** How many times to try a compensating action before giving up on it. */
    private static final int MAX_COMPENSATION_ATTEMPTS = 3;

    /** Linear backoff between attempts: 150ms, 300ms, ... - short, since this runs synchronously
     *  on the failure path of an already-failed request. */
    private static final long RETRY_BACKOFF_MS = 150;

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;

    public OrderSagaOrchestrator(
            OrderService orderService,
            PaymentService paymentService,
            InventoryService inventoryService,
            ShippingService shippingService) {

        this.orderService = orderService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.shippingService = shippingService;
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

        retry(sagaId, "cancelShipment", shipmentId, cause,
                "the shipment may still be scheduled",
                () -> shippingService.cancelShipment(shipmentId));

        retry(sagaId, "releaseStock", reservationId, cause,
                "the stock stays reserved and unavailable for sale",
                () -> inventoryService.releaseStock(reservationId));

        retry(sagaId, "refund", paymentId, cause,
                "the customer was charged and NOT refunded",
                () -> paymentService.refund(paymentId));

        retry(sagaId, "cancelOrder", orderId, cause,
                "the order stays in an inconsistent status",
                () -> orderService.cancelOrder(orderId, cause.getMessage()));
    }

    /**
     * Runs one compensating action, retrying transient failures a few times before giving
     * up. A blip in the target service (a dropped connection, a momentary timeout) is common
     * enough on the failure path - where we are already reacting to one problem - that it is
     * worth a couple of quick retries rather than immediately accepting the impact described
     * in {@code impactIfFailed}.
     *
     * <p>Never throws: if every attempt fails, the failure is logged at ERROR (naming the
     * step, the id it was acting on, the original failure that triggered compensation, and
     * the concrete impact) and execution moves on to the next compensation - one permanently
     * broken compensator must not stop the rest of the undo, and must never hide the
     * original failure from the caller of {@link #placeOrder}.</p>
     */
    private void retry(UUID sagaId, String stepName, Long targetId, Exception cause, String impactIfFailed,
                        Runnable action) {
        for (int attempt = 1; attempt <= MAX_COMPENSATION_ATTEMPTS; attempt++) {
            try {
                action.run();
                if (attempt > 1) {
                    log.warn("[saga={}] compensation {}(id={}) succeeded on retry attempt {}/{}",
                            sagaId, stepName, targetId, attempt, MAX_COMPENSATION_ATTEMPTS);
                }
                return;
            } catch (Exception e) {
                boolean lastAttempt = attempt == MAX_COMPENSATION_ATTEMPTS;
                if (lastAttempt) {
                    log.error("[saga={}] compensation FAILED after {} attempts: {}(id={}) - {}, manual cleanup "
                            + "needed. Original failure: {}",
                            sagaId, MAX_COMPENSATION_ATTEMPTS, stepName, targetId, impactIfFailed,
                            cause.getMessage(), e);
                } else {
                    log.warn("[saga={}] compensation attempt {}/{} failed: {}(id={}) - retrying. Error: {}",
                            sagaId, attempt, MAX_COMPENSATION_ATTEMPTS, stepName, targetId, e.getMessage());
                    if (!sleep(RETRY_BACKOFF_MS * attempt)) {
                        // Interrupted while waiting to retry - stop here rather than attempt
                        // more sleeps, but still go through the normal failure logging above
                        // by falling through to the next iteration, which will now be the
                        // last attempt.
                        log.warn("[saga={}] retry backoff for {}(id={}) was interrupted", sagaId, stepName,
                                targetId);
                    }
                }
            }
        }
    }

    /** @return true if the sleep completed normally, false if it was interrupted */
    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
