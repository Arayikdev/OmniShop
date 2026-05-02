package com.omnishop.orderservice.service;

import com.omnishop.orderservice.dto.*;
import com.omnishop.orderservice.entity.*;
import com.omnishop.orderservice.exception.*;
import com.omnishop.orderservice.payment.FakePaymentService;
import com.omnishop.orderservice.repository.CartItemRepository;
import com.omnishop.orderservice.repository.OrderItemRepository;
import com.omnishop.orderservice.repository.OrderRepository;
import com.omnishop.orderservice.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final FakePaymentService fakePaymentService;
    private final OrderEventPublisher orderEventPublisher;

    private final Counter ordersPlacedSuccess;
    private final Counter ordersFailedPayment;
    private final Counter ordersFailedInsufficientStock;
    private final Counter ordersFailedEmptyCart;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        FakePaymentService fakePaymentService,
                        OrderEventPublisher orderEventPublisher,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.fakePaymentService = fakePaymentService;
        this.orderEventPublisher = orderEventPublisher;
        this.ordersPlacedSuccess = Counter.builder("orders.placed.success")
                .description("Total orders successfully placed")
                .register(meterRegistry);
        this.ordersFailedPayment = Counter.builder("orders.placed.failed")
                .description("Total checkout attempts that failed")
                .tag("reason", "payment")
                .register(meterRegistry);
        this.ordersFailedInsufficientStock = Counter.builder("orders.placed.failed")
                .description("Total checkout attempts that failed")
                .tag("reason", "insufficient_stock")
                .register(meterRegistry);
        this.ordersFailedEmptyCart = Counter.builder("orders.placed.failed")
                .description("Total checkout attempts that failed")
                .tag("reason", "empty_cart")
                .register(meterRegistry);
    }

    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest request) {

        List<CartItem> cartItems = cartItemRepository.findByUserIdWithLock(userId);
        if (cartItems.isEmpty()) {
            ordersFailedEmptyCart.increment();
            throw new EmptyCartException("Cart is empty");
        }

        List<Product> products = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ProductNotAvailableException(
                            "Product not found: " + cartItem.getProductId()));

            if (!product.isActive()) {
                throw new ProductNotAvailableException(
                        "Product no longer available: " + product.getName());
            }

            if (product.getQuantity() < cartItem.getQuantity()) {
                ordersFailedInsufficientStock.increment();
                throw new InsufficientStockException(
                        "Only " + product.getQuantity() + " units available for: "
                        + product.getName());
            }

            products.add(product);
        }

        try {
            fakePaymentService.processPayment(request.getCardNumber());
        } catch (RuntimeException e) {
            ordersFailedPayment.increment();
            throw e;
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem cartItem = cartItems.get(i);
            Product product = products.get(i);

            int rows = productRepository.deductQuantity(
                    cartItem.getProductId(), cartItem.getQuantity());

            if (rows == 0) {

                ordersFailedInsufficientStock.increment();
                throw new InsufficientStockException(
                        "Only " + product.getQuantity() + " units available for: "
                        + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setDeliveryStatus(DeliveryStatus.PREPARING);
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(
                5 + (long) (Math.random() * 10)));
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteByUserId(userId);

        orderEventPublisher.publishOrderConfirmed(order, orderItems);

        ordersPlacedSuccess.increment();
        return buildOrderResponse(order, orderItems);
    }

    public Page<OrderSummary> getMyOrders(UUID userId, OrderStatus statusFilter, Pageable pageable) {
        int safePage = Math.min(Math.max(pageable.getPageNumber(), 0), 10000);
        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        Pageable safePageable = PageRequest.of(safePage, safeSize, pageable.getSort());
        Page<Order> page = (statusFilter != null)
                ? orderRepository.findByUserIdAndStatus(userId, statusFilter, safePageable)
                : orderRepository.findByUserId(userId, safePageable);
        return page.map(o -> new OrderSummary(
                o.getId(), o.getStatus(), o.getDeliveryStatus(), o.getTotalAmount(), o.getCreatedAt()));
    }

    public OrderResponse getOrder(UUID orderId, UUID requestingUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (!order.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("Access denied to order: " + orderId);
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return buildOrderResponse(order, items);
    }

    private OrderResponse buildOrderResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> new OrderItemResponse(
                        i.getProductId(), i.getProductName(), i.getQuantity(), i.getPriceAtPurchase()))
                .toList();
        return new OrderResponse(
                order.getId(), order.getUserId(), order.getStatus(), order.getDeliveryStatus(),
                order.getEstimatedDelivery(), order.getTotalAmount(), order.getCreatedAt(), itemResponses);
    }
}
