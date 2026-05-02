package com.omnishop.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CartItemResponse {

    private UUID productId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private boolean available;

    public CartItemResponse() {}

    public CartItemResponse(UUID productId, String name, int quantity,
                            BigDecimal unitPrice, BigDecimal subtotal, boolean available) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.available = available;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
