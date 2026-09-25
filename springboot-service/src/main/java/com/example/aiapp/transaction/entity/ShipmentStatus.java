package com.example.aiapp.transaction.entity;

/** Lifecycle of a shipment booked by the saga. */
public enum ShipmentStatus {
    SCHEDULED,
    CANCELLED,
    DELIVERED
}
