package com.example.rental.admin.dto;

import com.example.rental.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(@NotNull UserStatus status) {
}
