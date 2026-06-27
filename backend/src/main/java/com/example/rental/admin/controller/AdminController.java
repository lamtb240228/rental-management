package com.example.rental.admin.controller;

import com.example.rental.admin.dto.AdminSummaryResponse;
import com.example.rental.admin.dto.AdminUserResponse;
import com.example.rental.admin.service.AdminService;
import com.example.rental.common.response.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    ApiResponse<AdminSummaryResponse> summary() {
        return ApiResponse.of(adminService.summary());
    }

    @GetMapping("/users")
    ApiResponse<List<AdminUserResponse>> users() {
        return ApiResponse.of(adminService.users());
    }
}
