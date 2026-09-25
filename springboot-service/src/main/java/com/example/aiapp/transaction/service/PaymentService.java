package com.example.aiapp.transaction.service;

import java.math.BigDecimal;
import java.util.UUID;

/** Saga participant 3 - owns customer wallets and payments. */
public interface PaymentService {

    /**
     * Forward action of CHARGE_PAYMENT. Atomically debits the wallet and records a
     * CAPTURED payment.
     *
     * @return the new payment id, which is the handle the compensator needs
     * @throws com.example.aiapp.transaction.exception.PaymentDeclinedException if the
     *         wallet cannot cover the amount
     */
    Long charge(UUID sagaId, String orderRef, Long customerId, BigDecimal amount);

    /** Compensation for CHARGE_PAYMENT. Idempotent: a no-op if already refunded. */
    void refund(Long paymentId);
}
