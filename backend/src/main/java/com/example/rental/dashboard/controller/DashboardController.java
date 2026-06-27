package com.example.rental.dashboard.controller;

import com.example.rental.common.response.ApiResponse;
import com.example.rental.dashboard.dto.DashboardSummaryResponse;
import com.example.rental.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('LANDLORD')")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.of(dashboardService.summary());
    }
}
