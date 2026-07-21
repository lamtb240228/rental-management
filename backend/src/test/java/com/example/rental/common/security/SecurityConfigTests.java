package com.example.rental.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.rental.common.config.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTests {
    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void trimsExplicitCorsOrigins() {
        TrustedOriginPolicy trustedOriginPolicy = new TrustedOriginPolicy(
            new CorsProperties(" https://app.example.test , http://localhost:5173 ")
        );
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
            trustedOriginPolicy
        );
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
            .containsExactly("https://app.example.test", "http://localhost:5173");
        assertThat(trustedOriginPolicy.allowsOrigin("https://app.example.test")).isTrue();
        assertThat(trustedOriginPolicy.allowsReferer("https://app.example.test/account/security?tab=sessions")).isTrue();
        assertThat(trustedOriginPolicy.allowsOrigin("https://evil.example.test")).isFalse();
    }

    @Test
    void rejectsWildcardCorsOriginWhenCredentialsAreEnabled() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new TrustedOriginPolicy(new CorsProperties("*"))
        );
    }

    @Test
    void rejectsValuesThatAreNotExplicitOrigins() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new TrustedOriginPolicy(new CorsProperties("https://app.example.test/auth"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new TrustedOriginPolicy(new CorsProperties("https://*.example.test"))
        );
    }
}
