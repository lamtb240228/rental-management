package com.example.rental.common.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(
    @Min(1) @Max(365) long expirationDays,
    @Min(32) @Max(64) int tokenBytes,
    @NotBlank @Size(max = 64) @Pattern(regexp = "^[!#$%&'*+.^_`|~0-9A-Za-z-]+$") String cookieName,
    @NotBlank @Size(max = 255) @Pattern(regexp = "^/[^\\s;]*$") String cookiePath,
    boolean cookieSecure,
    @NotBlank @Pattern(regexp = "^(Strict|Lax)$") String cookieSameSite
) {
    @Override
    public String toString() {
        return "RefreshTokenProperties[expirationDays=" + expirationDays
            + ", tokenBytes=" + tokenBytes
            + ", cookieName=" + cookieName
            + ", cookiePath=" + cookiePath
            + ", cookieSecure=" + cookieSecure
            + ", cookieSameSite=" + cookieSameSite + "]";
    }
}
