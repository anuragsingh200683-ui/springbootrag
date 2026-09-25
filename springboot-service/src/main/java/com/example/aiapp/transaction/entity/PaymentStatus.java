package com.example.aiapp.transaction.entity;

/** Lifecycle of a payment taken by the saga. */
public enum PaymentStatus {
    CAPTURED,
    REFUNDED
}
