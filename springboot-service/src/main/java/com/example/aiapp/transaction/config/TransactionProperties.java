package com.example.aiapp.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
