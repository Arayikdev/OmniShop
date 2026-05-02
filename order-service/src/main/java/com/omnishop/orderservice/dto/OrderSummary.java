package com.omnishop.orderservice.dto;

import com.omnishop.orderservice.entity.DeliveryStatus;
import com.omnishop.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderSummary {

    private UUID id;
    private OrderStatus status;
    private DeliveryStatus deliveryStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public OrderSummary() {}

    public OrderSummary(UUID id, OrderStatus status, DeliveryStatus deliveryStatus,
                        BigDecimal totalAmount, LocalDateTime createdAt) {
        this.id = id;
        this.status = status;
        this.deliveryStatus = deliveryStatus;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
