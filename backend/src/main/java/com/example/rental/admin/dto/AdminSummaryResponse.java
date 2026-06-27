package com.example.rental.admin.dto;

public record AdminSummaryResponse(
    long userCount,
    long landlordCount,
    long tenantAccountCount,
    long propertyCount,
    long roomCount,
    long invoiceCount,
    long pendingMaintenanceCount
) {
}
