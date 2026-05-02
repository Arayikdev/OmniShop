package com.omnishop.orderservice.service;

import com.omnishop.orderservice.dto.CartItemRequest;
import com.omnishop.orderservice.dto.CartItemResponse;
import com.omnishop.orderservice.dto.CartResponse;
import com.omnishop.orderservice.entity.CartItem;
import com.omnishop.orderservice.entity.Product;
import com.omnishop.orderservice.exception.OrderNotFoundException;
import com.omnishop.orderservice.repository.CartItemRepository;
import com.omnishop.orderservice.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public CartResponse addToCart(UUID userId, CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new OrderNotFoundException("Product not found: " + request.getProductId()));
        if (!product.isActive()) {
            throw new OrderNotFoundException("Product not found: " + request.getProductId());
        }

        try {
            Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());
            if (existing.isPresent()) {
                CartItem item = existing.get();
                item.setQuantity(item.getQuantity() + request.getQuantity());
                cartItemRepository.save(item);
            } else {
                CartItem item = new CartItem();
                item.setUserId(userId);
                item.setProductId(request.getProductId());
                item.setQuantity(request.getQuantity());
                cartItemRepository.save(item);
            }
        } catch (DataIntegrityViolationException e) {
            CartItem existing = cartItemRepository
                    .findByUserIdAndProductId(userId, request.getProductId())
                    .orElseThrow();
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            cartItemRepository.save(existing);
        }
        return getCart(userId);
    }

    public CartResponse getCart(UUID userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> responseItems = new ArrayList<>();

        for (CartItem item : cartItems) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent() && productOpt.get().isActive()) {
                Product product = productOpt.get();
                BigDecimal unitPrice = product.getPrice();
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                responseItems.add(new CartItemResponse(
                        item.getProductId(), product.getName(),
                        item.getQuantity(), unitPrice, subtotal, true));
            } else {
                responseItems.add(new CartItemResponse(
                        item.getProductId(), null, item.getQuantity(), null, null, false));
            }
        }

        BigDecimal totalAmount = responseItems.stream()
                .filter(CartItemResponse::isAvailable)
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(responseItems, responseItems.size(), totalAmount);
    }

    public CartResponse updateCartItem(UUID userId, UUID productId, int quantity) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new OrderNotFoundException("Cart item not found for product: " + productId));
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return getCart(userId);
    }

    public CartResponse removeCartItem(UUID userId, UUID productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new OrderNotFoundException("Cart item not found for product: " + productId));
        cartItemRepository.delete(item);
        return getCart(userId);
    }

    public void clearCart(UUID userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
