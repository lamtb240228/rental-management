package com.example.rental.billing.controller;

import com.example.rental.billing.dto.UtilityReadingRequest;
import com.example.rental.billing.dto.UtilityReadingResponse;
import com.example.rental.billing.service.UtilityReadingService;
import com.example.rental.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('LANDLORD')")
public class UtilityReadingController {
    private final UtilityReadingService utilityReadingService;

    public UtilityReadingController(UtilityReadingService utilityReadingService) {
        this.utilityReadingService = utilityReadingService;
    }

    @GetMapping("/rooms/{roomId}/utility-readings")
    ApiResponse<List<UtilityReadingResponse>> list(@PathVariable Long roomId) {
        return ApiResponse.of(utilityReadingService.listByRoom(roomId));
    }

    @PostMapping("/rooms/{roomId}/utility-readings")
    ApiResponse<UtilityReadingResponse> create(@PathVariable Long roomId, @Valid @RequestBody UtilityReadingRequest request) {
        return ApiResponse.of(utilityReadingService.create(roomId, request));
    }

    @PutMapping("/utility-readings/{id}")
    ApiResponse<UtilityReadingResponse> update(@PathVariable Long id, @Valid @RequestBody UtilityReadingRequest request) {
        return ApiResponse.of(utilityReadingService.update(id, request));
    }
}
