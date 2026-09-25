package com.example.aiapp.transaction.entity;

/** Lifecycle of a stock reservation held on behalf of one saga. */
public enum ReservationStatus {
    /** Stock has been moved from available_qty to reserved_qty. */
    RESERVED,
    /** The saga compensated - the stock was handed back to available_qty. */
    RELEASED,
    /** The order shipped - the reserved stock left the warehouse for good. */
    CONSUMED
}
