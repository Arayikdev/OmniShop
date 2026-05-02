package com.omnishop.orderservice.repository;

import com.omnishop.orderservice.entity.DeliveryStatus;
import com.omnishop.orderservice.entity.Order;
import com.omnishop.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);

    Page<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff, Pageable pageable);

    List<Order> findByDeliveryStatusAndUpdatedAtBefore(DeliveryStatus status, LocalDateTime cutoff);

    List<Order> findByDeliveryStatusAndEstimatedDeliveryBefore(DeliveryStatus status, LocalDateTime time);
}
