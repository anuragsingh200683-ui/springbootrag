package com.example.aiapp.transaction.demo;

import com.example.aiapp.transaction.config.TransactionProperties;
import com.example.aiapp.transaction.dao.CustomerAccountDao;
import com.example.aiapp.transaction.dao.InventoryItemDao;
import com.example.aiapp.transaction.entity.CustomerAccount;
import com.example.aiapp.transaction.entity.InventoryItem;
import com.example.aiapp.transaction.model.PlaceOrderCommand;
import com.example.aiapp.transaction.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Walks the saga through one success and three distinct failures so the compensation chain
 * can be watched in the log.
 *
 * <p>Gated behind the {@code saga-demo} profile, so it never runs in a normal boot. Start
 * it with:</p>
 * <pre>mvn spring-boot:run -Dspring-boot.run.profiles=saga-demo</pre>
 */
@Component
@Profile("saga-demo")
@RequiredArgsConstructor
public class SagaDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SagaDemoRunner.class);

    private static final String SKU_PLENTIFUL = "DEMO-WIDGET";
    private static final String SKU_SCARCE = "DEMO-RARE";
    private static final Long CUSTOMER_RICH = 9001L;
    private static final Long CUSTOMER_BROKE = 9002L;

    private final OrderSagaOrchestrator orderSagaOrchestrator;
    private final InventoryItemDao inventoryItemDao;
    private final CustomerAccountDao customerAccountDao;
    private final TransactionProperties properties;

    @Override
    public void run(String... args) {
        if (properties.getDemo().isSeedData()) {
            seed();
        }

        banner("1/4 HAPPY PATH - all five steps commit, order ends CONFIRMED");
        runCase(command(SKU_PLENTIFUL, 2, CUSTOMER_RICH, "IN"));

        banner("2/4 OUT OF STOCK - fails at step 2, only CREATE_ORDER is compensated");
        runCase(command(SKU_SCARCE, 50, CUSTOMER_RICH, "IN"));

        banner("3/4 PAYMENT DECLINED - fails at step 3, stock is released and order cancelled");
        runCase(command(SKU_PLENTIFUL, 1, CUSTOMER_BROKE, "IN"));

        banner("4/4 SHIPPING UNAVAILABLE - fails at step 4; watch the refund, the stock "
                + "release and the cancellation fire in reverse order");
        runCase(command(SKU_PLENTIFUL, 1, CUSTOMER_RICH, "BR"));

        log.info("=== saga demo finished - inspect orders/payments/stock_reservations/shipments ===");
    }

    private void runCase(PlaceOrderCommand command) {
        try {
            orderSagaOrchestrator.placeOrder(command);
            log.info("RESULT: {} -> SUCCESS", command.orderRef());
        } catch (RuntimeException e) {
            log.info("RESULT: {} -> FAILED ({}: {})",
                    command.orderRef(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static PlaceOrderCommand command(String sku, int qty, Long customerId, String country) {
        return new PlaceOrderCommand(
                "DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                customerId,
                sku,
                qty,
                new BigDecimal("100.00"),
                country,
                "221B Baker Street, Demo City");
    }

    /**
     * Idempotent seeding: each row is only created if the SKU or customer is absent, so a
     * restart under the demo profile does not stack up duplicates or reset balances
     * mid-investigation.
     */
    private void seed() {
        if (inventoryItemDao.findBySku(SKU_PLENTIFUL).isEmpty()) {
            inventoryItemDao.save(InventoryItem.builder()
                    .sku(SKU_PLENTIFUL).description("Plentiful demo widget")
                    .availableQty(100).reservedQty(0).build());
        }
        if (inventoryItemDao.findBySku(SKU_SCARCE).isEmpty()) {
            inventoryItemDao.save(InventoryItem.builder()
                    .sku(SKU_SCARCE).description("Deliberately scarce demo item")
                    .availableQty(1).reservedQty(0).build());
        }
        if (customerAccountDao.findByCustomerId(CUSTOMER_RICH).isEmpty()) {
            customerAccountDao.save(CustomerAccount.builder()
                    .customerId(CUSTOMER_RICH).fullName("Well Funded")
                    .balance(new BigDecimal("100000.00")).currency("INR").build());
        }
        if (customerAccountDao.findByCustomerId(CUSTOMER_BROKE).isEmpty()) {
            customerAccountDao.save(CustomerAccount.builder()
                    .customerId(CUSTOMER_BROKE).fullName("Barely Funded")
                    .balance(new BigDecimal("1.00")).currency("INR").build());
        }
        log.info("Demo data seeded: SKUs [{}, {}], customers [{}, {}]",
                SKU_PLENTIFUL, SKU_SCARCE, CUSTOMER_RICH, CUSTOMER_BROKE);
    }

    private static void banner(String title) {
        log.info("========================================================================");
        log.info(">>> {}", title);
        log.info("========================================================================");
    }
}
