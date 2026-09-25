package com.example.aiapp.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * The receipt for a successful RESERVE_STOCK step.
 *
 * <p>The saga keeps only this row's id in its context; the compensator reads the row back
 * to learn what to hand over, which is what lets {@code releaseStock} be idempotent.</p>
 */
@Entity
@Table(name = "stock_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_reservations_saga_sku", columnNames = {"saga_id", "sku"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, updatable = false, length = 36)
    private String sagaId;

    @Column(name = "order_ref", nullable = false, updatable = false, length = 64)
    private String orderRef;

    @Column(nullable = false, updatable = false, length = 64)
    private String sku;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ReservationStatus.RESERVED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
