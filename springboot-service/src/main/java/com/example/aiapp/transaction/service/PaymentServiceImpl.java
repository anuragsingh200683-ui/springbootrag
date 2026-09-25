package com.example.aiapp.transaction.service;

import com.example.aiapp.transaction.dao.CustomerAccountDao;
import com.example.aiapp.transaction.dao.PaymentDao;
import com.example.aiapp.transaction.entity.CustomerAccount;
import com.example.aiapp.transaction.entity.PaymentEntity;
import com.example.aiapp.transaction.entity.PaymentStatus;
import com.example.aiapp.transaction.exception.PaymentDeclinedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment is the step that makes compensation feel real: the money has genuinely left the
 * wallet and been committed, so undoing it is a <b>refund</b> - a new, separately visible
 * business fact - not a rollback. Anyone reading the account between the charge and the
 * refund sees the lower balance, and that is correct and unavoidable.
 *
 * <p>This is the core trade-off of the pattern: a saga gives up isolation (the I in ACID)
 * in exchange for being able to span services at all. Intermediate states are observable.</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final CustomerAccountDao customerAccountDao;
    private final PaymentDao paymentDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long charge(UUID sagaId, String orderRef, Long customerId, BigDecimal amount) {
        Optional<CustomerAccount> account = customerAccountDao.findByCustomerId(customerId);
        if (account.isEmpty()) {
            throw new PaymentDeclinedException(orderRef, "no wallet found for customer " + customerId);
        }
        BigDecimal balanceBefore = account.get().getBalance();

        int debited = customerAccountDao.tryDebit(customerId, amount, Instant.now());
        if (debited == 0) {
            // No declined-payment row is written here on purpose: this method runs in its
            // own REQUIRES_NEW transaction, so anything persisted before the throw would be
            // rolled back with it. The failure is captured in saga_step_log instead, which
            // is written by a separate transaction that survives.
            throw new PaymentDeclinedException(orderRef,
                    "insufficient balance - have " + balanceBefore + ", need " + amount);
        }

        Instant now = Instant.now();
        PaymentEntity payment = PaymentEntity.builder()
                .sagaId(sagaId.toString())
                .paymentRef("PAY-" + sagaId)
                .orderRef(orderRef)
                .customerId(customerId)
                .amount(amount)
                .status(PaymentStatus.CAPTURED)
                .capturedAt(now)
                .build();

        Long paymentId = paymentDao.save(payment).getId();
        log.info("[saga={}] CHARGE_PAYMENT: captured {} from customer {} for order {} (paymentId={})",
                sagaId, amount, customerId, orderRef, paymentId);
        return paymentId;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refund(Long paymentId) {
        if (paymentId == null) {
            return;
        }
        Optional<PaymentEntity> found = paymentDao.findById(paymentId);
        if (found.isEmpty()) {
            log.warn("COMPENSATE refund: payment id={} no longer exists - nothing to undo", paymentId);
            return;
        }
        PaymentEntity payment = found.get();
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            // The guard that stops a double refund if this compensator is ever replayed.
            log.debug("COMPENSATE refund: payment id={} already {} - idempotent no-op",
                    paymentId, payment.getStatus());
            return;
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(now);
        paymentDao.save(payment);

        customerAccountDao.credit(payment.getCustomerId(), payment.getAmount(), now);
        log.info("COMPENSATE refund: returned {} to customer {} (paymentId={})",
                payment.getAmount(), payment.getCustomerId(), paymentId);
    }
}
