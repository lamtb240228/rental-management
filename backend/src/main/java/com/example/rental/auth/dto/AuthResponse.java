package com.example.rental.auth.dto;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UserProfileResponse user
) {
    @Override
    public String toString() {
        return "AuthResponse[accessToken=[REDACTED], tokenType=" + tokenType + ", user=" + user + "]";
    }
}
