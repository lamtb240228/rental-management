package com.example.rental.dashboard.dto;

public record DashboardSummaryResponse(
    long propertyCount,
    long availableRoomCount,
    long occupiedRoomCount,
    long invoiceCount,
    long pendingMaintenanceCount
) {
}
