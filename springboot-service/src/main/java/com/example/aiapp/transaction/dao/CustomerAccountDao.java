package com.example.aiapp.transaction.dao;

import com.example.aiapp.transaction.entity.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Data access for customer wallets.
 *
 * <p>Same conditional-UPDATE reasoning as {@link InventoryItemDao}: the balance check and
 * the debit have to be one statement, or two concurrent charges can both pass the check
 * and overdraw the account.</p>
 */
public interface CustomerAccountDao extends JpaRepository<CustomerAccount, Long> {

    Optional<CustomerAccount> findByCustomerId(Long customerId);

    /**
     * Atomically debit the wallet if - and only if - it can cover the amount.
     *
     * @return 1 if the debit happened, 0 if the balance was too low (or no such customer)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update CustomerAccount a
              set a.balance   = a.balance - :amount,
                  a.updatedAt = :now
            where a.customerId = :customerId
              and a.balance >= :amount
           """)
    int tryDebit(@Param("customerId") Long customerId,
                 @Param("amount") BigDecimal amount,
                 @Param("now") Instant now);

    /** The compensating action for {@link #tryDebit}: put the money back. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update CustomerAccount a
              set a.balance   = a.balance + :amount,
                  a.updatedAt = :now
            where a.customerId = :customerId
           """)
    int credit(@Param("customerId") Long customerId,
               @Param("amount") BigDecimal amount,
               @Param("now") Instant now);
}
