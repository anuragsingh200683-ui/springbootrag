package com.example.aiapp.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stock level for one SKU, owned by {@code InventoryService}.
 *
 * <p>Deliberately has no {@code @Version} column. Reservations are applied with a single
 * atomic conditional UPDATE ({@code InventoryItemDao.tryReserve}) rather than a
 * read-modify-write, so the database itself - not an optimistic-lock retry loop - is what
 * prevents two concurrent sagas from overselling the same unit.</p>
 */
@Entity
@Table(name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_items_sku", columnNames = "sku"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.availableQty == null) {
            this.availableQty = 0;
        }
        if (this.reservedQty == null) {
            this.reservedQty = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
