package com.example.aiapp.transaction.entity;

/**
 * Lifecycle of an order inside the place-order saga.
 *
 * <p>PENDING is the only state the saga creates up front; it becomes CONFIRMED when
 * every participant has committed, or CANCELLED when the saga compensates.</p>
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
