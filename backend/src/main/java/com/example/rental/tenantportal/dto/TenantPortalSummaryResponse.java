package com.example.rental.tenantportal.dto;

import com.example.rental.billing.dto.InvoiceResponse;
import com.example.rental.billing.dto.UtilityReadingResponse;
import com.example.rental.contract.dto.ContractResponse;
import com.example.rental.maintenance.dto.MaintenanceRequestResponse;
import com.example.rental.payment.dto.PaymentResponse;
import com.example.rental.property.dto.RoomResponse;
import com.example.rental.tenant.dto.TenantResponse;
import java.util.List;

public record TenantPortalSummaryResponse(
    TenantResponse tenant,
    RoomResponse room,
    ContractResponse activeContract,
    List<InvoiceResponse> invoices,
    List<PaymentResponse> payments,
    List<UtilityReadingResponse> utilityReadings,
    List<MaintenanceRequestResponse> maintenanceRequests
) {
}
