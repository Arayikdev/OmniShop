package com.omnishop.orderservice.controller;

import com.omnishop.orderservice.dto.CheckoutRequest;
import com.omnishop.orderservice.dto.OrderResponse;
import com.omnishop.orderservice.dto.OrderSummary;
import com.omnishop.orderservice.entity.OrderStatus;
import com.omnishop.orderservice.exception.AccessDeniedException;
import com.omnishop.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Place orders, track delivery, cancel orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Place an order",
               description = "Validates payment, fetches prices from catalog, saves order as PENDING, triggers async stock reservation via Kafka. Requires ROLE_CUSTOMER.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order placed, status PENDING"),
        @ApiResponse(responseCode = "400", description = "Empty cart, invalid card, or validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "403", description = "Insufficient role — ROLE_CUSTOMER required"),
        @ApiResponse(responseCode = "402", description = "Payment declined"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        readRole(httpRequest);
        UUID userId = readUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(userId, request));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "List my orders",
               description = "Returns a paginated list of orders for the authenticated customer. Optionally filter by order status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<OrderSummary>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        UUID userId = readUserId(httpRequest);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getMyOrders(userId, status, pageable));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID",
               description = "Returns full order details including line items and delivery status. Only the order owner can view it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "403", description = "Order belongs to a different user"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            HttpServletRequest httpRequest) {
        UUID userId = readUserId(httpRequest);
        return ResponseEntity.ok(orderService.getOrder(orderId, userId));
    }

    private UUID readUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("X-User-Id header is missing");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format for X-User-Id");
        }
    }

    private void readRole(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        if (!"ROLE_CUSTOMER".equals(role)) {
            throw new AccessDeniedException("Access denied: ROLE_CUSTOMER required");
        }
    }
}
