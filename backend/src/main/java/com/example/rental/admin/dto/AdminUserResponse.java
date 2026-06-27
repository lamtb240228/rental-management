package com.example.rental.admin.dto;

import com.example.rental.user.entity.UserStatus;
import java.time.OffsetDateTime;
import java.util.Set;

public record AdminUserResponse(
    Long id,
    String email,
    String fullName,
    String phone,
    UserStatus status,
    Set<String> roles,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt
) {
}
