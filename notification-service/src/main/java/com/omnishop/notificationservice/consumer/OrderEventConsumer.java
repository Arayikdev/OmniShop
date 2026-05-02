package com.omnishop.notificationservice.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnishop.notificationservice.dto.OrderConfirmedEvent;
import com.omnishop.notificationservice.dto.OrderDeliveredEvent;
import com.omnishop.notificationservice.entity.ProcessedEvent;
import com.omnishop.notificationservice.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class OrderEventConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    private final ProcessedEventRepository processedEventRepository;

    public OrderEventConsumer(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "notification-order-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(@Payload String payload) {
        try {
            Map<String, Object> raw = objectMapper.readValue(payload, new TypeReference<>() {});
            String eventType = detectEventType(raw);

            switch (eventType) {
                case "CONFIRMED" -> {
                    OrderConfirmedEvent event = objectMapper.readValue(payload, OrderConfirmedEvent.class);
                    if (processedEventRepository.existsById(event.eventId())) {
                        log.warn("Duplicate event ignored: {}", event.eventId());
                        return;
                    }
                    processedEventRepository.save(new ProcessedEvent(event.eventId(), LocalDateTime.now()));
                    log.info("ORDER CONFIRMED - orderId: {}, customer: {}, total: {}",
                            event.orderId(), event.customerId(), event.totalAmount());
                    log.info("Sending confirmation email to customer: {}", event.customerId());
                    log.info("Sending new order notification to seller: {}", event.sellerId());
                }
                case "DELIVERED" -> {
                    OrderDeliveredEvent event = objectMapper.readValue(payload, OrderDeliveredEvent.class);
                    if (processedEventRepository.existsById(event.eventId())) {
                        log.warn("Duplicate event ignored: {}", event.eventId());
                        return;
                    }
                    processedEventRepository.save(new ProcessedEvent(event.eventId(), LocalDateTime.now()));
                    log.info("Order DELIVERED - sending delivery confirmation to customer: {}",
                            event.customerId());
                    log.info("ORDER DELIVERED - orderId: {}, estimatedDelivery: {}",
                            event.orderId(), event.estimatedDelivery());
                }
                default -> log.warn("Unknown event type in payload: {}", payload);
            }
        } catch (Exception e) {
            log.error("Failed to process event: {}", payload, e);
        }
    }

    private String detectEventType(Map<String, Object> raw) {
        if (raw.containsKey("type")) {
            return String.valueOf(raw.get("type"));
        }

        if (raw.containsKey("totalAmount") && raw.containsKey("sellerId")) {
            return "CONFIRMED";
        } else if (raw.containsKey("estimatedDelivery")) {
            return "DELIVERED";
        }
        return "UNKNOWN";
    }
}
