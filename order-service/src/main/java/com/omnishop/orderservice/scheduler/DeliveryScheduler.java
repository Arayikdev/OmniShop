package com.omnishop.orderservice.scheduler;

import com.omnishop.orderservice.entity.DeliveryStatus;
import com.omnishop.orderservice.entity.Order;
import com.omnishop.orderservice.repository.OrderRepository;
import com.omnishop.orderservice.service.OrderEventPublisher;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public DeliveryScheduler(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Scheduled(fixedRate = 30000)
    @SchedulerLock(name = "delivery-scheduler", lockAtMostFor = "2m", lockAtLeastFor = "25s")
    public void advanceDelivery() {
        try {
            List<Order> preparing = orderRepository.findByDeliveryStatusAndUpdatedAtBefore(
                    DeliveryStatus.PREPARING, LocalDateTime.now().minusMinutes(1));
            for (Order order : preparing) {
                order.setDeliveryStatus(DeliveryStatus.SHIPPED);
                orderRepository.save(order);
            }
            log.info("Advanced {} orders to SHIPPED", preparing.size());

            List<Order> shipped = orderRepository.findByDeliveryStatusAndEstimatedDeliveryBefore(
                    DeliveryStatus.SHIPPED, LocalDateTime.now());
            for (Order order : shipped) {
                if (order.getEstimatedDelivery() == null) continue;
                order.setDeliveryStatus(DeliveryStatus.DELIVERED);
                orderRepository.save(order);
                orderEventPublisher.publishOrderDelivered(order);
            }
            log.info("Advanced {} orders to DELIVERED", shipped.size());
        } catch (Exception e) {
            log.error("DeliveryScheduler error: {}", e.getMessage(), e);
        }
    }
}
