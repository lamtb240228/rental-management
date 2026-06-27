package com.example.rental.billing.dto;

import com.example.rental.billing.entity.InvoiceItemType;
import java.math.BigDecimal;

public record InvoiceItemResponse(
    Long id,
    InvoiceItemType itemType,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal amount
) {
}
