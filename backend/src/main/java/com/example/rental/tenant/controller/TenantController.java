package com.example.rental.tenant.controller;

import com.example.rental.common.response.ApiResponse;
import com.example.rental.tenant.dto.TenantRequest;
import com.example.rental.tenant.dto.TenantResponse;
import com.example.rental.tenant.service.TenantService;
import com.example.rental.contract.dto.ContractResponse;
import com.example.rental.contract.service.ContractService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('LANDLORD')")
public class TenantController {
    private final TenantService tenantService;
    private final ContractService contractService;

    public TenantController(TenantService tenantService, ContractService contractService) {
        this.tenantService = tenantService;
        this.contractService = contractService;
    }

    @GetMapping
    ApiResponse<List<TenantResponse>> list() {
        return ApiResponse.of(tenantService.listMine());
    }

    @PostMapping
    ApiResponse<TenantResponse> create(@Valid @RequestBody TenantRequest request) {
        return ApiResponse.of(tenantService.create(request));
    }

    @GetMapping("/{id}")
    ApiResponse<TenantResponse> get(@PathVariable Long id) {
        return ApiResponse.of(tenantService.get(id));
    }

    @GetMapping("/{id}/contracts")
    ApiResponse<List<ContractResponse>> contracts(@PathVariable Long id) {
        return ApiResponse.of(contractService.listByTenant(id));
    }

    @PutMapping("/{id}")
    ApiResponse<TenantResponse> update(@PathVariable Long id, @Valid @RequestBody TenantRequest request) {
        return ApiResponse.of(tenantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id) {
        tenantService.delete(id);
    }
}
