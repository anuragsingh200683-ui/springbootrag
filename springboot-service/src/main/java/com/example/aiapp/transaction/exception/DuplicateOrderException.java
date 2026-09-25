package com.example.aiapp.transaction.exception;

/**
 * Thrown by the CREATE_ORDER step when the caller-supplied order reference already exists.
 *
 * <p>This is the guard that makes the saga safe to retry from the outside: replaying the
 * same command is rejected at the first step rather than quietly creating a second order
 * and charging the customer twice.</p>
 */
public class DuplicateOrderException extends RuntimeException {

    private final String orderRef;

    public DuplicateOrderException(String orderRef) {
        super("An order already exists with reference " + orderRef);
        this.orderRef = orderRef;
    }

    public String getOrderRef() {
        return orderRef;
    }
}
