package com.example.rental.tenantportal.dto;

import com.example.rental.maintenance.entity.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;

public record TenantMaintenanceCreateRequest(
    @NotBlank String title,
    @NotBlank String description,
    MaintenancePriority priority
) {
}
