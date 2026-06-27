package com.example.rental.contract.controller;

import com.example.rental.common.response.ApiResponse;
import com.example.rental.contract.dto.ContractRequest;
import com.example.rental.contract.dto.ContractResponse;
import com.example.rental.contract.service.ContractService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
@PreAuthorize("hasRole('LANDLORD')")
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    ApiResponse<List<ContractResponse>> list() {
        return ApiResponse.of(contractService.listMine());
    }

    @PostMapping
    ApiResponse<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return ApiResponse.of(contractService.create(request));
    }

    @GetMapping("/{id}")
    ApiResponse<ContractResponse> get(@PathVariable Long id) {
        return ApiResponse.of(contractService.get(id));
    }

    @PatchMapping("/{id}/end")
    ApiResponse<ContractResponse> end(
        @PathVariable Long id,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.of(contractService.end(id, endDate));
    }
}
