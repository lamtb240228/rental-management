package com.example.rental.property.dto;

import com.example.rental.property.entity.PropertyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PropertyRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 255) String addressLine,
    @Size(max = 100) String ward,
    @Size(max = 100) String district,
    @NotBlank @Size(max = 100) String provinceCity,
    String description,
    PropertyStatus status
) {
}
