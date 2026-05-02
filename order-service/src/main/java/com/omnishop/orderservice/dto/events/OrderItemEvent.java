package com.omnishop.orderservice.dto.events;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemEvent(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal priceAtPurchase
) {}
