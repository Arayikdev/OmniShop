package com.omnishop.orderservice.controller;

import com.omnishop.orderservice.dto.CartItemRequest;
import com.omnishop.orderservice.dto.CartResponse;
import com.omnishop.orderservice.dto.UpdateCartItemRequest;
import com.omnishop.orderservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@Tag(name = "Cart", description = "Shopping cart management — add, update, remove items and clear cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart",
               description = "Adds a product to the authenticated customer's cart. If the product already exists in the cart, quantity is incremented.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item added, updated cart returned"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody CartItemRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = readUserId(httpRequest);
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @GetMapping
    @Operation(summary = "View cart",
               description = "Returns the current cart contents with live availability and subtotal for each item.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CartResponse> getCart(HttpServletRequest httpRequest) {
        UUID userId = readUserId(httpRequest);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item quantity",
               description = "Sets the quantity of a specific product in the cart. Use quantity=0 to remove.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Product not in cart"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = readUserId(httpRequest);
        return ResponseEntity.ok(cartService.updateCartItem(userId, productId, request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart",
               description = "Removes a single product line from the cart.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item removed, updated cart returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Product not in cart"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable UUID productId,
            HttpServletRequest httpRequest) {
        UUID userId = readUserId(httpRequest);
        return ResponseEntity.ok(cartService.removeCartItem(userId, productId));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart",
               description = "Removes all items from the authenticated customer's cart.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cart cleared"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> clearCart(HttpServletRequest httpRequest) {
        UUID userId = readUserId(httpRequest);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
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
}
