package com.example.aiapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter =
            new KeycloakJwtAuthenticationConverter("realm_access.roles");

    @Test
    void mapsRealmRolesAndScopesToAuthoritiesAndUsesPreferredUsername() {
        Jwt jwt = jwt(Map.of(
                "sub", "8f1c-uuid",
                "preferred_username", "admin1",
                "scope", "openid profile",
                "realm_access", Map.of("roles", List.of("USER", "ADMIN"))));

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo("admin1");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "SCOPE_openid", "SCOPE_profile");
    }

    @Test
    void tokenWithoutRolesOrUsernameGetsNoRolesAndFallsBackToSubject() {
        Jwt jwt = jwt(Map.of("sub", "service-account-uuid"));

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo("service-account-uuid");
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void malformedRolesClaimIsIgnored() {
        Jwt jwt = jwt(Map.of("sub", "x", "realm_access", "not-a-map"));

        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
