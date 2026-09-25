package com.example.aiapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * By default Spring Boot only scans the package this class lives in
 * (com.example.aiapp) and its sub-packages for components, which already covers the
 * new com.example.aiapp.transaction saga module's services. But @EntityScan and
 * @EnableJpaRepositories are pinned to com.example.aiapp.entity and
 * com.example.aiapp.repository specifically, so the transaction module's entities and
 * DAOs have to be added to them explicitly or they are never registered.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EntityScan(basePackages = {"com.example.aiapp.entity", "com.example.aiapp.transaction.entity"})
@EnableJpaRepositories(basePackages = {"com.example.aiapp.repository", "com.example.aiapp.transaction.dao"})
public class AiAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAppApplication.class, args);
    }
}
