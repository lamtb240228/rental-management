package com.example.rental.maintenance.controller;

import com.example.rental.common.response.ApiResponse;
import com.example.rental.maintenance.dto.MaintenanceRequestCreateRequest;
import com.example.rental.maintenance.dto.MaintenanceRequestResponse;
import com.example.rental.maintenance.dto.MaintenanceStatusUpdateRequest;
import com.example.rental.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance-requests")
@PreAuthorize("hasRole('LANDLORD')")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    ApiResponse<List<MaintenanceRequestResponse>> list() {
        return ApiResponse.of(maintenanceService.listMine());
    }

    @PostMapping
    ApiResponse<MaintenanceRequestResponse> create(@Valid @RequestBody MaintenanceRequestCreateRequest request) {
        return ApiResponse.of(maintenanceService.create(request));
    }

    @PatchMapping("/{id}/status")
    ApiResponse<MaintenanceRequestResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody MaintenanceStatusUpdateRequest request
    ) {
        return ApiResponse.of(maintenanceService.updateStatus(id, request));
    }
}
