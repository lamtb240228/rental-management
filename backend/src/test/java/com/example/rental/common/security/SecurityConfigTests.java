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
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
            new CorsProperties(" https://app.example.test , http://localhost:5173 ")
        );
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
            .containsExactly("https://app.example.test", "http://localhost:5173");
    }

    @Test
    void rejectsWildcardCorsOriginWhenCredentialsAreEnabled() {
        assertThrows(
            IllegalArgumentException.class,
            () -> securityConfig.corsConfigurationSource(new CorsProperties("*"))
        );
    }
}
