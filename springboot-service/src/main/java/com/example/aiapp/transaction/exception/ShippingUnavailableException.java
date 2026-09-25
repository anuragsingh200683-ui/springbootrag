package com.example.aiapp.transaction.exception;

import java.util.Collection;

/** Thrown by the SCHEDULE_SHIPMENT step when the destination is outside the served region. */
public class ShippingUnavailableException extends RuntimeException {

    private final String orderRef;
    private final String destinationCountry;

    public ShippingUnavailableException(String orderRef, String destinationCountry,
                                        Collection<String> serviceableCountries) {
        super("Shipping unavailable for order " + orderRef + " to country '" + destinationCountry
                + "'; serviceable countries are " + serviceableCountries);
        this.orderRef = orderRef;
        this.destinationCountry = destinationCountry;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }
}
