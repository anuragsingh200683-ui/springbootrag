package com.example.aiapp.transaction.service;

import com.example.aiapp.transaction.dao.OrderDao;
import com.example.aiapp.transaction.entity.OrderEntity;
import com.example.aiapp.transaction.entity.OrderLineItem;
import com.example.aiapp.transaction.entity.OrderStatus;
import com.example.aiapp.transaction.exception.DuplicateOrderException;
import com.example.aiapp.transaction.model.PlaceOrderCommand;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Every mutating method here is {@code REQUIRES_NEW}.
 *
 * <p>That is the rule that makes the saga work. Each step must commit independently and
 * stay committed even when a later step blows up, because the whole premise of the pattern
 * is that there is no outer transaction to roll back - the damage is undone by running a
 * compensating action, not by the transaction manager.</p>
 *
 * <p>{@code REQUIRES_NEW} rather than the default {@code REQUIRED} also protects against a
 * caller that does hold a transaction: without it, such a caller would silently absorb all
 * five steps into one unit of work, and a failure would roll back the saga log along with
 * the business data, leaving no record that anything happened.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final int MAX_REASON_LENGTH = 500;

    private final OrderDao orderDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createOrder(UUID sagaId, PlaceOrderCommand command) {
        if (orderDao.existsByOrderRef(command.orderRef())) {
            throw new DuplicateOrderException(command.orderRef());
        }

        BigDecimal total = command.totalAmount();

        OrderEntity order = OrderEntity.builder()
                .orderRef(command.orderRef())
                .sagaId(sagaId.toString())
                .customerId(command.customerId())
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .build();

        order.addLineItem(OrderLineItem.builder()
                .sku(command.sku())
                .quantity(command.quantity())
                .unitPrice(command.unitPrice())
                .lineTotal(total)
                .build());

        OrderEntity saved = orderDao.save(order);
        log.info("[saga={}] CREATE_ORDER: order id={} ref={} total={} status=PENDING",
                sagaId, saved.getId(), saved.getOrderRef(), total);
        return saved.getId();
    }

    /**
     * Idempotent by design: it re-reads the order and returns quietly if it is already
     * cancelled or simply gone. A compensator can be invoked more than once (a retry, a
     * recovery sweep after a crash), and it must never fail or double-apply when that
     * happens.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelOrder(Long orderId, String reason) {
        if (orderId == null) {
            return;
        }
        Optional<OrderEntity> found = orderDao.findById(orderId);
        if (found.isEmpty()) {
            log.warn("COMPENSATE cancelOrder: order id={} no longer exists - nothing to undo", orderId);
            return;
        }
        OrderEntity order = found.get();
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("COMPENSATE cancelOrder: order id={} already CANCELLED - idempotent no-op", orderId);
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(truncate(reason));
        orderDao.save(order);
        log.info("COMPENSATE cancelOrder: order id={} -> CANCELLED ({})", orderId, reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmOrder(Long orderId) {
        OrderEntity order = orderDao.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Cannot confirm missing order id=" + orderId));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.debug("CONFIRM_ORDER: order id={} already CONFIRMED - idempotent no-op", orderId);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm order id=" + orderId + " in status " + order.getStatus());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderDao.save(order);
        log.info("CONFIRM_ORDER: order id={} -> CONFIRMED", orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<OrderEntity> findByOrderRef(String orderRef) {
        return orderDao.findByOrderRef(orderRef);
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= MAX_REASON_LENGTH ? reason : reason.substring(0, MAX_REASON_LENGTH);
    }
}
