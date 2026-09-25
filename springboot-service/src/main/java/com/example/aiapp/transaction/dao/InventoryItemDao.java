package com.example.aiapp.transaction.dao;

import com.example.aiapp.transaction.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Data access for stock levels.
 *
 * <p>The reserve/release pair is expressed as conditional bulk UPDATEs rather than
 * load-mutate-save on purpose. A read-modify-write would let two concurrent sagas both
 * read available_qty = 1 and both conclude the unit is theirs; a single
 * {@code UPDATE ... WHERE available_qty >= :qty} is evaluated by the database under a row
 * lock, so exactly one of them affects a row and the other gets 0 back and is told there
 * is no stock. No optimistic-lock retry loop needed.</p>
 *
 * <p>{@code clearAutomatically} / {@code flushAutomatically} are set because a bulk UPDATE
 * bypasses the persistence context: pending changes must be flushed before it runs, and
 * already-loaded copies must be evicted after it, or callers would keep reading stale
 * quantities. Note this also means entity callbacks ({@code @PreUpdate}) do not fire,
 * which is why updated_at is assigned explicitly in the statement.</p>
 */
public interface InventoryItemDao extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findBySku(String sku);

    /**
     * Atomically move qty from available to reserved.
     *
     * @return 1 if the stock was reserved, 0 if there was not enough (or no such SKU)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update InventoryItem i
              set i.availableQty = i.availableQty - :qty,
                  i.reservedQty  = i.reservedQty  + :qty,
                  i.updatedAt    = :now
            where i.sku = :sku
              and i.availableQty >= :qty
           """)
    int tryReserve(@Param("sku") String sku, @Param("qty") int qty, @Param("now") Instant now);

    /**
     * The compensating action for {@link #tryReserve}: hand the stock back.
     *
     * @return 1 if the SKU existed, 0 otherwise
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update InventoryItem i
              set i.availableQty = i.availableQty + :qty,
                  i.reservedQty  = i.reservedQty  - :qty,
                  i.updatedAt    = :now
            where i.sku = :sku
           """)
    int releaseReserved(@Param("sku") String sku, @Param("qty") int qty, @Param("now") Instant now);
}
