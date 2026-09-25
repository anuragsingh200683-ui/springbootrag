package com.example.aiapp.transaction.saga;

import com.example.aiapp.transaction.model.PlaceOrderCommand;
import com.example.aiapp.transaction.service.InventoryService;
import com.example.aiapp.transaction.service.OrderService;
import com.example.aiapp.transaction.service.PaymentService;
import com.example.aiapp.transaction.service.ShippingService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderSagaOrchestrator {

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

            compensate(orderId, paymentId, reservationId, shipmentId, e);

            throw e;
        }
    }

    private void compensate(Long orderId, Long paymentId, Long reservationId, Long shipmentId, Exception cause) {

        try {
            shippingService.cancelShipment(shipmentId);
        } catch (Exception e) {
            // retry / recovery
        }

        try {
            inventoryService.releaseStock(reservationId);
        } catch (Exception e) {
            // retry / recovery
        }

        try {
            paymentService.refund(paymentId);
        } catch (Exception e) {
            // retry / recovery
        }

        try {
            orderService.cancelOrder(orderId, cause.getMessage());
        } catch (Exception e) {
            // retry / recovery
        }
    }
}
