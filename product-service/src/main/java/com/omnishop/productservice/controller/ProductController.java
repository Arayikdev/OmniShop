package com.omnishop.productservice.controller;

import com.omnishop.productservice.dto.*;
import com.omnishop.productservice.exception.AccessDeniedException;
import com.omnishop.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Product management and inventory")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products", description = "Returns paginated active products. Public endpoint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Products returned"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Page<ProductSummaryDTO> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return productService.getAll(page, size, search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns full product details. Public endpoint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductDetailDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product. Requires ROLE_SELLER.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "ROLE_SELLER required")
    })
    public ResponseEntity<ProductDetailDTO> create(
            @Valid @RequestBody ProductRequest request,
            HttpServletRequest httpRequest) {
        requireRole(httpRequest);
        UUID sellerId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request, sellerId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates name, description, price. Requires ROLE_SELLER and ownership.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductDetailDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request,
            HttpServletRequest httpRequest) {
        requireRole(httpRequest);
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(productService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft-deletes a product. Requires ROLE_SELLER and ownership.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        requireRole(httpRequest);
        UUID userId = extractUserId(httpRequest);
        productService.softDelete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/quantity")
    @Operation(summary = "Get product quantity", description = "Returns quantity for seller's own product.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quantity returned"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Map<String, Object>> getQuantity(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        requireRole(httpRequest);
        UUID userId = extractUserId(httpRequest);
        ProductDetailDTO detail = productService.getQuantity(id, userId);
        return ResponseEntity.ok(Map.of(
                "productId", detail.id(),
                "sellerId", detail.sellerId(),
                "quantity", detail.quantity()
        ));
    }

    @PutMapping("/{id}/quantity")
    @Operation(summary = "Set quantity", description = "Sets absolute quantity. Requires ROLE_SELLER and ownership.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quantity updated"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Map<String, Object>> setQuantity(
            @PathVariable UUID id,
            @Valid @RequestBody QuantityUpdateRequest request,
            HttpServletRequest httpRequest) {
        requireRole(httpRequest);
        UUID userId = extractUserId(httpRequest);
        ProductDetailDTO detail = productService.setQuantity(id, request, userId);
        return ResponseEntity.ok(Map.of(
                "productId", detail.id(),
                "quantity", detail.quantity()
        ));
    }

    @PatchMapping("/{id}/quantity/add")
    @Operation(summary = "Add to quantity", description = "Adds delta to existing quantity. Requires ROLE_SELLER and ownership.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quantity updated"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Map<String, Object>> addQuantity(
            @PathVariable UUID id,
            @Valid @RequestBody QuantityAddRequest request,
            HttpServletRequest httpRequest) {
        requireRole(httpRequest);
        UUID userId = extractUserId(httpRequest);
        ProductDetailDTO detail = productService.addQuantity(id, request, userId);
        return ResponseEntity.ok(Map.of(
                "productId", detail.id(),
                "quantity", detail.quantity()
        ));
    }

    private UUID extractUserId(HttpServletRequest request) {
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

    private void requireRole(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        if (!"ROLE_SELLER".equals(role)) {
            throw new AccessDeniedException("Access denied: ROLE_SELLER required");
        }
    }
}
