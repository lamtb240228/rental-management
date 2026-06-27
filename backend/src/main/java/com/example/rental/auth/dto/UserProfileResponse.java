package com.example.rental.auth.dto;

import java.util.Set;

public record UserProfileResponse(
    Long id,
    String email,
    String fullName,
    String phone,
    Set<String> roles
) {
}
