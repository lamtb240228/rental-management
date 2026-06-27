package com.example.rental.property.dto;

import com.example.rental.property.entity.RoomStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RoomResponse(
    Long id,
    Long propertyId,
    String propertyName,
    String roomNumber,
    Integer floorNumber,
    BigDecimal area,
    BigDecimal monthlyRent,
    BigDecimal defaultDeposit,
    Short maxOccupants,
    RoomStatus status,
    String description,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
