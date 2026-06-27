package com.example.rental.auth.dto;

import java.util.Set;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UserProfileResponse user
) {
}
