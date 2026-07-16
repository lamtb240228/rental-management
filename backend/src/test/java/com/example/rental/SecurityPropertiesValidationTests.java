package com.example.rental;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.rental.common.config.CorsProperties;
import com.example.rental.common.security.JwtProperties;
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
        }
    }
}
