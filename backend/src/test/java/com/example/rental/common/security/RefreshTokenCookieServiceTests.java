package com.example.rental.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class RefreshTokenCookieServiceTests {
    @Test
    void productionCookieIsAlwaysHttpOnlySecureAndStrict() {
        RefreshTokenCookieService service = new RefreshTokenCookieService(new RefreshTokenProperties(
            7,
            32,
            "rental_refresh",
            "/api/auth",
            true,
            "Strict"
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clear(response);

        assertThat(response.getHeader("Set-Cookie"))
            .contains("rental_refresh=")
            .contains("Path=/api/auth")
            .contains("Max-Age=0")
            .contains("Secure")
            .contains("HttpOnly")
            .contains("SameSite=Strict")
            .doesNotContain("Domain=");
    }
}
