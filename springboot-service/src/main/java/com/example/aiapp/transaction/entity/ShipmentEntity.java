package com.example.aiapp.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** The receipt for a successful SCHEDULE_SHIPMENT step. */
@Entity
@Table(name = "shipments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipments_saga_id", columnNames = "saga_id"),
                @UniqueConstraint(name = "uk_shipments_tracking_number", columnNames = "tracking_number")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, updatable = false, length = 36)
    private String sagaId;

    @Column(name = "tracking_number", nullable = false, unique = true, updatable = false, length = 64)
    private String trackingNumber;

    @Column(name = "order_ref", nullable = false, updatable = false, length = 64)
    private String orderRef;

    @Column(name = "destination_country", nullable = false, updatable = false, length = 2)
    private String destinationCountry;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

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
            this.status = ShipmentStatus.SCHEDULED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
