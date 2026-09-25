package com.example.aiapp.transaction.service;

import java.util.UUID;

/** Saga participant 2 - owns stock levels and reservations. */
public interface InventoryService {

    /**
     * Forward action of RESERVE_STOCK. Atomically moves stock from available to reserved
     * and records a reservation row.
     *
     * @return the new reservation id, which is the handle the compensator needs
     * @throws com.example.aiapp.transaction.exception.InsufficientStockException if short
     */
    Long reserveStock(UUID sagaId, String orderRef, String sku, int quantity);

    /** Compensation for RESERVE_STOCK. Idempotent: a no-op if already released. */
    void releaseStock(Long reservationId);
}
