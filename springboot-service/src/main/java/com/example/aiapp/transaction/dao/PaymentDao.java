package com.example.aiapp.transaction.dao;

import com.example.aiapp.transaction.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for payments. */
public interface PaymentDao extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findBySagaId(String sagaId);

    Optional<PaymentEntity> findByOrderRef(String orderRef);
}
