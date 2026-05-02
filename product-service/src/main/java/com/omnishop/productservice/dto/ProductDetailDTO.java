package com.omnishop.productservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductDetailDTO(
        UUID id,
        UUID sellerId,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        LocalDateTime createdAt
) {}
