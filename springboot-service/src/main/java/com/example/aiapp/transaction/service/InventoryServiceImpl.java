package com.example.aiapp.transaction.service;

import com.example.aiapp.transaction.dao.InventoryItemDao;
import com.example.aiapp.transaction.dao.StockReservationDao;
import com.example.aiapp.transaction.entity.InventoryItem;
import com.example.aiapp.transaction.entity.ReservationStatus;
import com.example.aiapp.transaction.entity.StockReservation;
import com.example.aiapp.transaction.exception.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Stock is a classic saga participant: the reservation is a <b>semantic lock</b>.
 *
 * <p>The saga cannot hold a database lock on the inventory row for its whole duration -
 * that is exactly the distributed-lock problem the pattern exists to avoid. So instead of
 * locking, it moves the quantity into a {@code reserved_qty} bucket that nobody else can
 * sell, and the compensator moves it back. The lock is expressed in the data model rather
 * than in the transaction manager.</p>
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryItemDao inventoryItemDao;
    private final StockReservationDao stockReservationDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long reserveStock(UUID sagaId, String orderRef, String sku, int quantity) {
        Optional<InventoryItem> item = inventoryItemDao.findBySku(sku);
        if (item.isEmpty()) {
            throw new InsufficientStockException(sku, quantity, 0);
        }
        // Read the current level BEFORE the bulk update: tryReserve clears the persistence
        // context, which detaches this instance.
        int availableBefore = item.get().getAvailableQty();

        int updated = inventoryItemDao.tryReserve(sku, quantity, Instant.now());
        if (updated == 0) {
            // The conditional UPDATE matched no row, so the WHERE clause failed - there was
            // not enough stock. Nothing was written, so there is nothing to undo here.
            throw new InsufficientStockException(sku, quantity, availableBefore);
        }

        StockReservation reservation = StockReservation.builder()
                .sagaId(sagaId.toString())
                .orderRef(orderRef)
                .sku(sku)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .build();

        Long reservationId = stockReservationDao.save(reservation).getId();
        log.info("[saga={}] RESERVE_STOCK: reserved {} x {} for order {} (reservationId={})",
                sagaId, quantity, sku, orderRef, reservationId);
        return reservationId;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseStock(Long reservationId) {
        if (reservationId == null) {
            return;
        }
        Optional<StockReservation> found = stockReservationDao.findById(reservationId);
        if (found.isEmpty()) {
            log.warn("COMPENSATE releaseStock: reservation id={} no longer exists - nothing to undo",
                    reservationId);
            return;
        }
        StockReservation reservation = found.get();
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.debug("COMPENSATE releaseStock: reservation id={} already {} - idempotent no-op",
                    reservationId, reservation.getStatus());
            return;
        }

        Instant now = Instant.now();
        // Mark the reservation first, then run the bulk UPDATE: @Modifying(flushAutomatically)
        // pushes this pending change out before the statement, and clearAutomatically then
        // detaches everything, so doing it the other way round would lose the status change.
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(now);
        stockReservationDao.save(reservation);

        inventoryItemDao.releaseReserved(reservation.getSku(), reservation.getQuantity(), now);
        log.info("COMPENSATE releaseStock: returned {} x {} to available stock (reservationId={})",
                reservation.getQuantity(), reservation.getSku(), reservationId);
    }
}
