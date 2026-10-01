package com.example.aiapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Turns a validated Keycloak access token into a Spring Security authentication.
 * <p>
 * Keycloak puts realm roles under a nested claim ({@code realm_access.roles} by default),
 * which Spring's stock converter does not read - it only understands {@code scope}/{@code scp}.
 * Each role becomes a {@code ROLE_<name>} authority so {@code hasRole("ADMIN")} works; the
 * standard scope authorities ({@code SCOPE_*}) are kept alongside them.
 * <p>
 * The principal name is {@code preferred_username} (the Keycloak login name), falling back
 * to {@code sub} for tokens that do not carry it (e.g. client-credentials tokens).
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
    private final String[] rolesClaimPath;

    public KeycloakJwtAuthenticationConverter(
            @Value("${app.security.jwt.roles-claim:realm_access.roles}") String rolesClaim) {
        this.rolesClaimPath = rolesClaim.split("\\.");
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopesConverter.convert(jwt));
        for (String role : extractRoles(jwt)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        String username = jwt.getClaimAsString("preferred_username");
        return new JwtAuthenticationToken(jwt, authorities, username != null ? username : jwt.getSubject());
    }

    private Collection<String> extractRoles(Jwt jwt) {
        Object current = jwt.getClaims();
        for (String segment : rolesClaimPath) {
            if (!(current instanceof Map<?, ?> map)) {
                return Set.of();
            }
            current = map.get(segment);
        }
        if (!(current instanceof Collection<?> values)) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) {
                roles.add(value.toString());
            }
        }
        return roles;
    }
}
