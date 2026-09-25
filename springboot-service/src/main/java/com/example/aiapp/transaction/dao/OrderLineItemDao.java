package com.example.aiapp.transaction.dao;

import com.example.aiapp.transaction.entity.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for order lines. Present mainly so tests can assert on them directly. */
public interface OrderLineItemDao extends JpaRepository<OrderLineItem, Long> {

    List<OrderLineItem> findByOrderId(Long orderId);
}
