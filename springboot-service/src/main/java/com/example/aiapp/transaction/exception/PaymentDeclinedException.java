package com.example.aiapp.transaction.exception;

/** Thrown by the CHARGE_PAYMENT step when the wallet cannot cover the order total. */
public class PaymentDeclinedException extends RuntimeException {

    private final String orderRef;

    public PaymentDeclinedException(String orderRef, String reason) {
        super("Payment declined for order " + orderRef + ": " + reason);
        this.orderRef = orderRef;
    }

    public String getOrderRef() {
        return orderRef;
    }
}
