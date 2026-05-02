package com.omnishop.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal totalAmount;

    public CartResponse() {}

    public CartResponse(List<CartItemResponse> items, int totalItems, BigDecimal totalAmount) {
        this.items = items;
        this.totalItems = totalItems;
        this.totalAmount = totalAmount;
    }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
