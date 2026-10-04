package com.example.aiapp.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Configuration for the saga transaction module, bound from {@code app.transaction.*}.
 *
 * <p>Picked up by the {@code @ConfigurationPropertiesScan} already declared on
 * {@code AiAppApplication}, which scans {@code com.example.aiapp} and below.</p>
 */
@ConfigurationProperties(prefix = "app.transaction")
@Getter
@Setter
public class TransactionProperties {

    private Shipping shipping = new Shipping();
    private Demo demo = new Demo();
    private Compensation compensation = new Compensation();

    @Getter
    @Setter
    public static class Shipping {

        /** ISO-3166 alpha-2 codes the carrier will deliver to. Compared case-insensitively. */
        private Set<String> serviceableCountries = new LinkedHashSet<>(Set.of("IN", "US", "GB", "DE"));
    }

    @Getter
    @Setter
    public static class Demo {

        /**
         * Whether the saga-demo profile should seed inventory and wallet rows on startup.
         * Ignored unless the {@code saga-demo} profile is active.
         */
        private boolean seedData = true;
    }

    @Getter
    @Setter
    public static class Compensation {

        /** How many times to try one compensating action before giving up on it. Values below 1 are treated as 1. */
        private int maxAttempts = 3;

        /** Wait before retry n is {@code backoff * n}: 150ms, then 300ms with the defaults. */
        private Duration backoff = Duration.ofMillis(150);

        /**
         * Total time one compensation run may spend before it stops retrying. Once it is used up,
         * each remaining compensating action still gets exactly one attempt. This keeps a full
         * database outage from multiplying the connection-timeout wait by {@code maxAttempts}.
         */
        private Duration retryBudget = Duration.ofSeconds(2);
    }
}
