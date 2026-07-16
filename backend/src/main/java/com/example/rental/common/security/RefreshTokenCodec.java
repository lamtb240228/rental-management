package com.example.rental.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCodec {
    private static final int MAX_PRESENTED_TOKEN_LENGTH = 512;

    private final SecureRandom secureRandom = new SecureRandom();
    private final int tokenBytes;

    public RefreshTokenCodec(RefreshTokenProperties properties) {
        this.tokenBytes = properties.tokenBytes();
    }

    public RefreshTokenValue generate() {
        byte[] randomBytes = new byte[tokenBytes];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return new RefreshTokenValue(rawToken, hash(rawToken));
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > MAX_PRESENTED_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Refresh token is malformed");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static final class RefreshTokenValue {
        private final String rawToken;
        private final String tokenHash;

        private RefreshTokenValue(String rawToken, String tokenHash) {
            this.rawToken = rawToken;
            this.tokenHash = tokenHash;
        }

        public String rawToken() {
            return rawToken;
        }

        public String tokenHash() {
            return tokenHash;
        }

        @Override
        public String toString() {
            return "RefreshTokenValue[rawToken=[REDACTED], tokenHash=[REDACTED]]";
        }
    }
}
