package com.example.rental.property.dto;

import com.example.rental.property.entity.RoomStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RoomRequest(
    @NotBlank String roomNumber,
    Integer floorNumber,
    @NotNull @DecimalMin(value = "0.01") BigDecimal area,
    @NotNull @DecimalMin(value = "0.00") BigDecimal monthlyRent,
    @NotNull @DecimalMin(value = "0.00") BigDecimal defaultDeposit,
    @NotNull @Positive Short maxOccupants,
    RoomStatus status,
    String description
) {
}
