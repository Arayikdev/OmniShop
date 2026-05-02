package com.omnishop.orderservice.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public UUID getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public Integer getQuantity() { return quantity; }
    public boolean isActive() { return isActive; }
}
