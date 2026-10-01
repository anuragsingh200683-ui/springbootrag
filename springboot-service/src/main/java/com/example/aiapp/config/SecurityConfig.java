package com.example.aiapp.config;

import com.example.aiapp.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * JWT bearer-token security. This service is an OAuth2 Resource Server only: users and
 * passwords live in the external auth server (Keycloak, realm {@code aiapp}), which issues
 * signed access tokens. Each request's {@code Authorization: Bearer <jwt>} is validated
 * locally against the auth server's published keys (JWKS) plus issuer, audience and expiry
 * - see {@code spring.security.oauth2.resourceserver.jwt} in application.yml - and the
 * token's realm roles are mapped to authorities by {@link KeycloakJwtAuthenticationConverter}.
 * <p>
 * Authorization is URL-based on purpose: it rejects the request in the filter chain before
 * any controller runs, so GlobalExceptionHandler's catch-all can never turn an
 * AccessDeniedException into a 500.
 * <p>
 * CORS is opened up for the React dev servers on http://localhost:3000 (aiapp)
 * and http://localhost:5173 (ems-app).
 */
@Configuration
public class SecurityConfig {

    private static final String[] EMS_ADMIN_RESOURCES = {
            "/api/ems/departments/**", "/api/ems/designations/**", "/api/ems/employees/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           KeycloakJwtAuthenticationConverter jwtConverter,
                                           ObjectMapper objectMapper) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // "/swagger-ui.html" (the actual entry page, not just the /swagger-ui/**
                        // static assets it redirects to) and "/v3/api-docs/**" for the raw spec.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // CORS preflight carries no Authorization header.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Admin-only: master-data writes, leave decisions, document deletion.
                        .requestMatchers(HttpMethod.POST, EMS_ADMIN_RESOURCES).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, EMS_ADMIN_RESOURCES).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, EMS_ADMIN_RESOURCES).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ems/leaves/*/approve", "/api/ems/leaves/*/reject")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/documents/**").hasRole("ADMIN")

                        // Everything else under /api: any signed-in user.
                        .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)));

        return http.build();
    }

    /** 401 - missing, malformed, expired, wrongly-signed or wrong-audience token. */
    private AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            writeError(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                    "Missing or invalid bearer token.");
        };
    }

    /** 403 - valid token, but the caller's roles don't allow this endpoint. */
    private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, ex) -> writeError(objectMapper, request, response, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.");
    }

    private void writeError(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // localhost:3000 = existing CRA app (aiapp), localhost:5173 = new ems-app Vite dev server
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
