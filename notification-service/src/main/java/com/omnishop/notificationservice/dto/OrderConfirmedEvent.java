package com.omnishop.notificationservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(
        String type,
        UUID eventId,
        UUID orderId,
        UUID customerId,
        UUID sellerId,
        List<OrderItem> items,
        BigDecimal totalAmount,
        LocalDateTime confirmedAt
) {
    public record OrderItem(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal priceAtPurchase
    ) {}
}
