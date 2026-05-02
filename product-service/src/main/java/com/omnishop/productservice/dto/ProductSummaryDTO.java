package com.omnishop.productservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryDTO(
        UUID id,
        String name,
        BigDecimal price,
        String descriptionPreview
) {}
