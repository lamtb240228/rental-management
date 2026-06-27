package com.example.rental.tenantportal.controller;

import com.example.rental.common.response.ApiResponse;
import com.example.rental.maintenance.dto.MaintenanceRequestResponse;
import com.example.rental.tenantportal.dto.TenantMaintenanceCreateRequest;
import com.example.rental.tenantportal.dto.TenantPortalSummaryResponse;
import com.example.rental.tenantportal.service.TenantPortalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant-portal")
@PreAuthorize("hasRole('TENANT')")
public class TenantPortalController {
    private final TenantPortalService tenantPortalService;

    public TenantPortalController(TenantPortalService tenantPortalService) {
        this.tenantPortalService = tenantPortalService;
    }

    @GetMapping("/summary")
    ApiResponse<TenantPortalSummaryResponse> summary() {
        return ApiResponse.of(tenantPortalService.summary());
    }

    @PostMapping("/maintenance-requests")
    ApiResponse<MaintenanceRequestResponse> createMaintenance(@Valid @RequestBody TenantMaintenanceCreateRequest request) {
        return ApiResponse.of(tenantPortalService.createMaintenance(request));
    }
}
