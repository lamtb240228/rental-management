package com.example.rental.common.security;

import com.example.rental.auth.service.RefreshSessionGrant;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieService {
    private final RefreshTokenProperties properties;

    public RefreshTokenCookieService(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public void write(HttpServletResponse response, RefreshSessionGrant grant) {
        Duration remainingLifetime = Duration.between(Instant.now(), grant.expiresAt().toInstant());
        if (remainingLifetime.isNegative() || remainingLifetime.isZero()) {
            clear(response);
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(grant.rawToken(), remainingLifetime).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    public String cookieName() {
        return properties.cookieName();
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(properties.cookieName(), value)
            .httpOnly(true)
            .secure(properties.cookieSecure())
            .sameSite(properties.cookieSameSite())
            .path(properties.cookiePath())
            .maxAge(maxAge)
            .build();
    }
}
