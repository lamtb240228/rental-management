package com.example.rental.property.dto;

import com.example.rental.property.entity.PropertyStatus;
import java.time.OffsetDateTime;

public record PropertyResponse(
    Long id,
    String name,
    String addressLine,
    String ward,
    String district,
    String provinceCity,
    String description,
    PropertyStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
