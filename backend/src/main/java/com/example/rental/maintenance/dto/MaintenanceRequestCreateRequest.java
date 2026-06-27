package com.example.rental.maintenance.dto;

import com.example.rental.maintenance.entity.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaintenanceRequestCreateRequest(
    @NotNull Long roomId,
    Long tenantId,
    @NotBlank String title,
    @NotBlank String description,
    MaintenancePriority priority
) {
}
