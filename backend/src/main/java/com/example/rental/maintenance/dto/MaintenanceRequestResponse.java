package com.example.rental.maintenance.dto;

import com.example.rental.maintenance.entity.MaintenancePriority;
import com.example.rental.maintenance.entity.MaintenanceStatus;
import java.time.OffsetDateTime;

public record MaintenanceRequestResponse(
    Long id,
    Long roomId,
    String roomNumber,
    Long tenantId,
    String tenantName,
    Long createdBy,
    String title,
    String description,
    MaintenancePriority priority,
    MaintenanceStatus status,
    OffsetDateTime submittedAt,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    OffsetDateTime cancelledAt,
    String resolutionNotes
) {
}
