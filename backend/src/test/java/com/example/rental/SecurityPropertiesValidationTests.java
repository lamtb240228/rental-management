package com.example.rental;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.rental.common.config.CorsProperties;
import com.example.rental.common.security.JwtProperties;
import com.example.rental.common.security.RefreshTokenProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class SecurityPropertiesValidationTests {
    @Test
    void rejectsWeakJwtAndBlankCorsConfiguration() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(new JwtProperties("too-short", 0)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("secret", "expirationMinutes");
            assertThat(validator.validate(new CorsProperties(" ")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("allowedOrigins");
            assertThat(validator.validate(new RefreshTokenProperties(
                0,
                16,
                "invalid cookie name",
                "not-a-path",
                false,
                "None"
            )))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                    "expirationDays",
                    "tokenBytes",
                    "cookieName",
                    "cookiePath",
                    "cookieSameSite"
                );
        }
    }

    @Test
    void acceptsValidSecurityConfiguration() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(new JwtProperties(
                "a-secure-jwt-secret-with-at-least-32-characters",
                60
            ))).isEmpty();
            assertThat(validator.validate(new CorsProperties("https://app.example.test"))).isEmpty();
            assertThat(validator.validate(new RefreshTokenProperties(
                7,
                32,
                "rental_refresh",
                "/api/auth",
                true,
                "Strict"
            ))).isEmpty();
        }
    }
}
