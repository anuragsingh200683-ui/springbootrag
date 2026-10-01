package com.example.aiapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * By default Spring Boot only scans the package this class lives in
 * (com.example.aiapp) plus its sub-packages. The new Employee Management
 * System module lives in the sibling package com.example.employeemanagement,
 * so component/entity/repository scanning is widened explicitly below.
 * Nothing about the existing aiapp scanning changes.
 *
 * The com.example.aiapp.transaction saga module IS under com.example.aiapp, so
 * @ComponentScan already covers it - but @EntityScan and @EnableJpaRepositories
 * are pinned to the aiapp.entity / aiapp.repository packages specifically, so its
 * entities and DAOs have to be listed explicitly too or they are never registered.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(basePackages = {"com.example.aiapp", "com.example.employeemanagement"})
@EntityScan(basePackages = {"com.example.aiapp.entity", "com.example.aiapp.transaction.entity",
        "com.example.employeemanagement"})
@EnableJpaRepositories(basePackages = {"com.example.aiapp.repository", "com.example.aiapp.transaction.dao",
        "com.example.employeemanagement"})
public class AiAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAppApplication.class, args);
    }
}
