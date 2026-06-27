package com.example.rental.maintenance.dto;

import com.example.rental.maintenance.entity.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;

public record MaintenanceStatusUpdateRequest(
    @NotNull MaintenanceStatus status,
    String resolutionNotes
) {
}
