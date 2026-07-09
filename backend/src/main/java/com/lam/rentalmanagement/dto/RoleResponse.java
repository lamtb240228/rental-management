package com.lam.rentalmanagement.dto;

import com.lam.rentalmanagement.domain.entity.Role;

public record RoleResponse (
        Long id,
        String name,
        String description
) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription()
        );
    }
}

