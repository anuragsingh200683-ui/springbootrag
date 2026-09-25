package com.example.aiapp.transaction.service;

import com.example.aiapp.transaction.entity.OrderEntity;
import com.example.aiapp.transaction.model.PlaceOrderCommand;

import java.util.Optional;
import java.util.UUID;

/**
 * Saga participant 1 - owns the order aggregate.
 *
 * <p>Note the shape of the API: plain domain arguments in, an id out. The service knows
 * nothing about sagas, step ordering or compensation - the orchestrator adapts between
 * this API and the saga context. That keeps the participant independently testable and
 * reusable outside the saga.</p>
 */
public interface OrderService {

    /**
     * Forward action of CREATE_ORDER. Commits a PENDING order in its own transaction.
     *
     * @return the new order id
     * @throws com.example.aiapp.transaction.exception.DuplicateOrderException if orderRef exists
     */
    Long createOrder(UUID sagaId, PlaceOrderCommand command);

    /** Compensation for CREATE_ORDER. Idempotent: a no-op if already cancelled. */
    void cancelOrder(Long orderId, String reason);

    /** Forward action of CONFIRM_ORDER, the saga pivot. */
    void confirmOrder(Long orderId);

    Optional<OrderEntity> findByOrderRef(String orderRef);
}
