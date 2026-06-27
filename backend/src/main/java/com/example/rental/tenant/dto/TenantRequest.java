package com.example.rental.tenant.dto;

import com.example.rental.tenant.entity.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TenantRequest(
    @NotBlank @Size(max = 150) String fullName,
    LocalDate dateOfBirth,
    @Size(max = 20) String phone,
    @Email String email,
    @Size(max = 50) String identityNumber,
    String permanentAddress,
    TenantStatus status
) {
}
