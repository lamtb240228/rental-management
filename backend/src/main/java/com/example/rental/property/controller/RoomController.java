package com.example.rental.property.controller;

import com.example.rental.common.response.ApiResponse;
import com.example.rental.property.dto.RoomRequest;
import com.example.rental.property.dto.RoomResponse;
import com.example.rental.property.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@PreAuthorize("hasRole('LANDLORD')")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/{id}")
    ApiResponse<RoomResponse> get(@PathVariable Long id) {
        return ApiResponse.of(roomService.get(id));
    }

    @PutMapping("/{id}")
    ApiResponse<RoomResponse> update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.of(roomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
