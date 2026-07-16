package com.example.rental.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenRedactionTests {
    @Test
    void generatedRefreshTokenAndHashAreRedactedFromStringRepresentation() {
        RefreshTokenCodec codec = new RefreshTokenCodec(new RefreshTokenProperties(
            7,
            32,
            "rental_refresh",
            "/api/auth",
            false,
            "Strict"
        ));
        RefreshTokenCodec.RefreshTokenValue token = codec.generate();

        assertThat(token.toString())
            .doesNotContain(token.rawToken())
            .doesNotContain(token.tokenHash())
            .contains("rawToken=[REDACTED]", "tokenHash=[REDACTED]");
    }
}
