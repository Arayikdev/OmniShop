package com.omnishop.orderservice.dto.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderDeliveredEvent(
        String type,
        UUID eventId,
        UUID orderId,
        UUID customerId,
        LocalDateTime estimatedDelivery
) {}
