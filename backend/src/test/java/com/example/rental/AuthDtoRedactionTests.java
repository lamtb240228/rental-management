package com.example.rental;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.rental.auth.dto.AuthResponse;
import com.example.rental.auth.dto.ChangePasswordRequest;
import com.example.rental.auth.dto.LoginRequest;
import com.example.rental.auth.dto.RegisterRequest;
import com.example.rental.auth.dto.UserProfileResponse;
import com.example.rental.common.config.BootstrapAdminProperties;
import com.example.rental.common.security.JwtProperties;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthDtoRedactionTests {
    @Test
    void authDtoStringRepresentationsDoNotExposePasswordOrAccessToken() {
        String password = "NeverLogThisPassword!";
        String accessToken = "never.log.this.jwt";
        String jwtSecret = "never-log-this-jwt-secret-value-123456";
        UserProfileResponse profile = new UserProfileResponse(
            1L,
            "security@rental.local",
            "Security Test",
            null,
            Set.of("LANDLORD")
        );

        assertThat(new LoginRequest("security@rental.local", password).toString())
            .doesNotContain(password)
            .contains("password=[REDACTED]");
        assertThat(new RegisterRequest("security@rental.local", password, "Security Test", null).toString())
            .doesNotContain(password)
            .contains("password=[REDACTED]");
        assertThat(new ChangePasswordRequest(password, "AnotherNeverLoggedPassword!").toString())
            .doesNotContain(password)
            .doesNotContain("AnotherNeverLoggedPassword!")
            .contains("currentPassword=[REDACTED]", "newPassword=[REDACTED]");
        assertThat(new AuthResponse(accessToken, "Bearer", profile).toString())
            .doesNotContain(accessToken)
            .contains("accessToken=[REDACTED]");
        assertThat(new BootstrapAdminProperties("admin@example.invalid", password, "Admin").toString())
            .doesNotContain(password)
            .contains("password=[REDACTED]");
        assertThat(new JwtProperties(jwtSecret, 60).toString())
            .doesNotContain(jwtSecret)
            .contains("secret=[REDACTED]");
    }
}
