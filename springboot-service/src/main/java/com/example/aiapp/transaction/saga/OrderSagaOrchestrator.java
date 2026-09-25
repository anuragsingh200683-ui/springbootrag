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

        try {
            shippingService.cancelShipment(shipmentId);
        } catch (Exception e) {
            log.error("[saga={}] compensation FAILED: cancelShipment(shipmentId={}) - manual cleanup needed. "
                    + "Original failure: {}", sagaId, shipmentId, cause.getMessage(), e);
        }

        try {
            inventoryService.releaseStock(reservationId);
        } catch (Exception e) {
            log.error("[saga={}] compensation FAILED: releaseStock(reservationId={}) - manual cleanup needed. "
                    + "Original failure: {}", sagaId, reservationId, cause.getMessage(), e);
        }

        try {
            paymentService.refund(paymentId);
        } catch (Exception e) {
            log.error("[saga={}] compensation FAILED: refund(paymentId={}) - customer was charged and NOT "
                    + "refunded, manual cleanup needed. Original failure: {}",
                    sagaId, paymentId, cause.getMessage(), e);
        }

        try {
            orderService.cancelOrder(orderId, cause.getMessage());
        } catch (Exception e) {
            log.error("[saga={}] compensation FAILED: cancelOrder(orderId={}) - manual cleanup needed. "
                    + "Original failure: {}", sagaId, orderId, cause.getMessage(), e);
        }
    }
}
