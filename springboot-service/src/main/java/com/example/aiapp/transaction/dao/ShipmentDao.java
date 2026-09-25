package com.example.aiapp.transaction.dao;

import com.example.aiapp.transaction.entity.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for shipments. */
public interface ShipmentDao extends JpaRepository<ShipmentEntity, Long> {

    Optional<ShipmentEntity> findBySagaId(String sagaId);

    Optional<ShipmentEntity> findByOrderRef(String orderRef);
}
