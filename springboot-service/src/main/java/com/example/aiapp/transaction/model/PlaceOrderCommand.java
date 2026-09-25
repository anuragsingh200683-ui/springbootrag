package com.example.aiapp.transaction.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The input to the place-order saga.
 *
 * <p>A record because it is an immutable value object that is passed around between the
 * orchestrator and four services - the same reason {@code aiapp.ingest.ChunkRecord} is one.
 * The REST controller happens to accept it directly as a request body rather than
 * introducing a near-identical DTO.</p>
 *
 * <p>{@code orderRef} is supplied by the caller and is the saga's <b>idempotency key</b>:
 * it is unique in the orders table, so replaying the same command fails fast at the first
 * step instead of charging the customer twice.</p>
 */
public record PlaceOrderCommand(

        @NotBlank(message = "orderRef is required")
        @Size(max = 64, message = "orderRef must be at most 64 characters")
        String orderRef,

        @NotNull(message = "customerId is required")
        Long customerId,

        @NotBlank(message = "sku is required")
        @Size(max = 64, message = "sku must be at most 64 characters")
        String sku,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.01", message = "unitPrice must be greater than zero")
        BigDecimal unitPrice,

        @NotBlank(message = "destinationCountry is required")
        @Size(min = 2, max = 2, message = "destinationCountry must be a 2-letter ISO country code")
        String destinationCountry,

        @NotBlank(message = "shippingAddress is required")
        @Size(max = 500, message = "shippingAddress must be at most 500 characters")
        String shippingAddress) {

    /** Order total, rounded to 2dp - the amount the payment step will try to capture. */
    public BigDecimal totalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
