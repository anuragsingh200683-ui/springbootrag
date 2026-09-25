package com.example.aiapp.transaction.service;

import java.util.UUID;

/** Saga participant 4 - books and cancels shipments. */
public interface ShippingService {

    /**
     * Forward action of SCHEDULE_SHIPMENT.
     *
     * @return the new shipment id, which is the handle the compensator needs
     * @throws com.example.aiapp.transaction.exception.ShippingUnavailableException if the
     *         destination country is not served
     */
    Long scheduleShipment(UUID sagaId, String orderRef, String destinationCountry, String shippingAddress);

    /** Compensation for SCHEDULE_SHIPMENT. Idempotent: a no-op if already cancelled. */
    void cancelShipment(Long shipmentId);
}
