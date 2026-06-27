package com.example.rental.payment.controller;

import com.example.rental.billing.dto.InvoiceResponse;
import com.example.rental.common.response.ApiResponse;
import com.example.rental.payment.dto.PaymentRequest;
import com.example.rental.payment.dto.PaymentResponse;
import com.example.rental.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/payments")
@PreAuthorize("hasRole('LANDLORD')")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    ApiResponse<List<PaymentResponse>> list(@PathVariable Long invoiceId) {
        return ApiResponse.of(paymentService.listByInvoice(invoiceId));
    }

    @PostMapping
    ApiResponse<InvoiceResponse> create(@PathVariable Long invoiceId, @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.of(paymentService.create(invoiceId, request));
    }
}
