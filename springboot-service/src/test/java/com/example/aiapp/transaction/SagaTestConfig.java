package com.example.aiapp.transaction;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * A narrow context holding only the transaction module.
 *
 * <p>Booting {@code AiAppApplication} instead would drag in Redis, Spring Security and the
 * OpenAI client, none of which this module touches - and two of which need a live server or
 * an API key. Naming this class explicitly in {@code @SpringBootTest(classes = ...)} stops
 * the usual upward search for a {@code @SpringBootConfiguration} and keeps the saga tests
 * self-contained and fast.</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.example.aiapp.transaction")
@EntityScan(basePackages = "com.example.aiapp.transaction.entity")
@EnableJpaRepositories(basePackages = "com.example.aiapp.transaction.dao")
@ConfigurationPropertiesScan(basePackages = "com.example.aiapp.transaction.config")
public class SagaTestConfig {
}
