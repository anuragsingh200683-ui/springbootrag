package com.example.employeemanagement.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI metadata for the EMS module.
 * UI is served by springdoc at /swagger-ui.html (aggregates every controller,
 * including the existing aiapp ones, under one spec).
 * Declares a global JWT bearer scheme so Swagger UI shows an "Authorize" button
 * for pasting an access token issued by the auth server.
 */
@Configuration
public class EmsOpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI emsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Management System API")
                        .description("REST API for Department, Designation, Employee, Attendance, "
                                + "Leave, Dashboard and AI Assistant modules.")
                        .version("1.0.0")
                        .contact(new Contact().name("EMS Team")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
