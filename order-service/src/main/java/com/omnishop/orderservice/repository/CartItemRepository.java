package com.omnishop.orderservice.repository;

import com.omnishop.orderservice.entity.CartItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartItem c WHERE c.userId = :userId")
    List<CartItem> findByUserIdWithLock(@Param("userId") UUID userId);

    Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId);

    @Transactional
    void deleteByUserId(UUID userId);

    @Transactional
    void deleteByUserIdAndProductId(UUID userId, UUID productId);

    @Transactional
    void deleteByAddedAtBefore(LocalDateTime cutoff);
}
