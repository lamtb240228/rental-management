package com.example.rental.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class JwtAuthVersionTests {
    private final JwtService jwtService = new JwtService(
        new JwtProperties("test-secret-change-this-value-to-at-least-32-characters", 15)
    );

    @Test
    void tokenIsRejectedAfterAuthenticationVersionChanges() {
        UserPrincipal issuedPrincipal = principal(7);
        String token = jwtService.generateToken(issuedPrincipal);

        assertThat(jwtService.isValid(token, issuedPrincipal)).isTrue();
        assertThat(jwtService.isValid(token, principal(8))).isFalse();
    }

    private UserPrincipal principal(long authVersion) {
        return new UserPrincipal(
            42L,
            "person@example.test",
            "unused-password-hash",
            "Test Person",
            authVersion,
            List.of(),
            true
        );
    }
}
