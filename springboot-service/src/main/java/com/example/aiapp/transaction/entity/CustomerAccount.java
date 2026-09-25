package com.example.aiapp.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Prepaid wallet balance owned by {@code PaymentService}.
 *
 * <p>This is what makes CHARGE_PAYMENT a real step rather than a stub: the charge debits a
 * real balance and the refund credits it back, so a test can assert the money returned.</p>
 */
@Entity
@Table(name = "customer_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_accounts_customer_id", columnNames = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, updatable = false)
    private Long customerId;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
        if (this.currency == null) {
            this.currency = "INR";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
