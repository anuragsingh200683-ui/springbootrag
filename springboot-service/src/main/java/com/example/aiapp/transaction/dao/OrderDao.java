package com.example.aiapp.transaction.dao;

import com.example.aiapp.transaction.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for the order aggregate. */
public interface OrderDao extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderRef(String orderRef);

    boolean existsByOrderRef(String orderRef);

    Optional<OrderEntity> findBySagaId(String sagaId);
}
