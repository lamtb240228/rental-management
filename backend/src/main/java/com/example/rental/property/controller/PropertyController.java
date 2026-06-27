package com.example.rental.property.controller;

import com.example.rental.common.response.ApiResponse;
import com.example.rental.property.dto.PropertyRequest;
import com.example.rental.property.dto.PropertyResponse;
import com.example.rental.property.dto.RoomRequest;
import com.example.rental.property.dto.RoomResponse;
import com.example.rental.property.service.PropertyService;
import com.example.rental.property.service.RoomService;
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
@RequestMapping("/api/properties")
@PreAuthorize("hasRole('LANDLORD')")
public class PropertyController {
    private final PropertyService propertyService;
    private final RoomService roomService;

    public PropertyController(PropertyService propertyService, RoomService roomService) {
        this.propertyService = propertyService;
        this.roomService = roomService;
    }

    @GetMapping
    ApiResponse<List<PropertyResponse>> list() {
        return ApiResponse.of(propertyService.listMine());
    }

    @PostMapping
    ApiResponse<PropertyResponse> create(@Valid @RequestBody PropertyRequest request) {
        return ApiResponse.of(propertyService.create(request));
    }

    @GetMapping("/{id}")
    ApiResponse<PropertyResponse> get(@PathVariable Long id) {
        return ApiResponse.of(propertyService.get(id));
    }

    @PutMapping("/{id}")
    ApiResponse<PropertyResponse> update(@PathVariable Long id, @Valid @RequestBody PropertyRequest request) {
        return ApiResponse.of(propertyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id) {
        propertyService.delete(id);
    }

    @GetMapping("/{propertyId}/rooms")
    ApiResponse<List<RoomResponse>> listRooms(@PathVariable Long propertyId) {
        return ApiResponse.of(roomService.listByProperty(propertyId));
    }

    @PostMapping("/{propertyId}/rooms")
    ApiResponse<RoomResponse> createRoom(@PathVariable Long propertyId, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.of(roomService.create(propertyId, request));
    }
}
