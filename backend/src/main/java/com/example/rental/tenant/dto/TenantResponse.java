package com.example.rental.tenant.dto;

import com.example.rental.tenant.entity.TenantStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TenantResponse(
    Long id,
    String fullName,
    LocalDate dateOfBirth,
    String phone,
    String email,
    String identityNumber,
    String permanentAddress,
    TenantStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
