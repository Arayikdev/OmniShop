package com.omnishop.orderservice.service;

import com.omnishop.orderservice.dto.events.*;
import com.omnishop.orderservice.entity.Order;
import com.omnishop.orderservice.entity.OrderItem;
import com.omnishop.orderservice.entity.Product;
import com.omnishop.orderservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductRepository productRepository;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                ProductRepository productRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.productRepository = productRepository;
    }

    public void publishOrderConfirmed(Order order, List<OrderItem> items) {
        UUID sellerId = productRepository.findById(items.get(0).getProductId())
                .map(Product::getSellerId)
                .orElse(null);

        List<OrderItemEvent> itemEvents = items.stream()
                .map(i -> new OrderItemEvent(
                        i.getProductId(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getPriceAtPurchase()))
                .toList();

        OrderConfirmedEvent event = new OrderConfirmedEvent(
                "CONFIRMED",
                UUID.randomUUID(),
                order.getId(),
                order.getUserId(),
                sellerId,
                itemEvents,
                order.getTotalAmount(),
                LocalDateTime.now());

        kafkaTemplate.send("order-events", order.getId().toString(), event);
        log.info("Published OrderConfirmedEvent for orderId: {}", order.getId());
    }

    public void publishOrderDelivered(Order order) {
        OrderDeliveredEvent event = new OrderDeliveredEvent(
                "DELIVERED",
                UUID.randomUUID(),
                order.getId(),
                order.getUserId(),
                order.getEstimatedDelivery());

        kafkaTemplate.send("order-events", order.getId().toString(), event);
        log.info("Published OrderDeliveredEvent for orderId: {}", order.getId());
    }
}
