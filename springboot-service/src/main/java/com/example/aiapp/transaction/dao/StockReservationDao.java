package com.example.aiapp.transaction.dao;

import com.example.aiapp.transaction.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for stock reservations. */
public interface StockReservationDao extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findBySagaIdAndSku(String sagaId, String sku);
}
