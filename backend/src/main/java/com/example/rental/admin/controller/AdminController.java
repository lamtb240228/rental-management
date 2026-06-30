package com.example.rental.admin.controller;

import com.example.rental.admin.dto.AdminSummaryResponse;
import com.example.rental.admin.dto.AdminUserResponse;
import com.example.rental.admin.dto.AdminUserStatusUpdateRequest;
import com.example.rental.admin.service.AdminService;
import com.example.rental.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PatchMapping("/users/{id}/status")
    ApiResponse<AdminUserResponse> updateUserStatus(
        @PathVariable Long id,
        @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        return ApiResponse.of(adminService.updateUserStatus(id, request));
    }
}
