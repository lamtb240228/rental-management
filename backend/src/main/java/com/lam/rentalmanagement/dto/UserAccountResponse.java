package com.lam.rentalmanagement.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.lam.rentalmanagement.domain.entity.Role;
import com.lam.rentalmanagement.domain.entity.UserAccount;
import com.lam.rentalmanagement.domain.entity.UserAccountStatus;

public record UserAccountResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        UserAccountStatus status,
        boolean emailVerified,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserAccountResponse from(UserAccount userAccount) {
        List<String> roleNames = userAccount.getRoles()
                .stream()
                .map(Role::getName)
                .sorted()
                .toList();

        return new UserAccountResponse(
                userAccount.getId(),
                userAccount.getEmail(),
                userAccount.getFullName(),
                userAccount.getPhone(),
                userAccount.getStatus(),
                userAccount.isEmailVerified(),
                roleNames,
                userAccount.getCreatedAt(),
                userAccount.getUpdatedAt()
        );
    }
}