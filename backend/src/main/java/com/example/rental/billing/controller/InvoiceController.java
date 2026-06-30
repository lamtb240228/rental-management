package com.example.rental.billing.controller;

import com.example.rental.billing.dto.InvoiceRequest;
import com.example.rental.billing.dto.InvoiceResponse;
import com.example.rental.billing.service.InvoiceService;
import com.example.rental.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasRole('LANDLORD')")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    ApiResponse<List<InvoiceResponse>> list() {
        return ApiResponse.of(invoiceService.listMine());
    }

    @PostMapping
    ApiResponse<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ApiResponse.of(invoiceService.create(request));
    }

    @GetMapping("/{id}")
    ApiResponse<InvoiceResponse> get(@PathVariable Long id) {
        return ApiResponse.of(invoiceService.get(id));
    }

    @PatchMapping("/{id}/cancel")
    ApiResponse<InvoiceResponse> cancel(@PathVariable Long id) {
        return ApiResponse.of(invoiceService.cancel(id));
    }
}
