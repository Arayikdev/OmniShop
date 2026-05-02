package com.omnishop.orderservice.repository;

import com.omnishop.orderservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.quantity = p.quantity - :qty " +
           "WHERE p.id = :productId AND p.quantity >= :qty AND p.isActive = true")
    int deductQuantity(@Param("productId") UUID productId, @Param("qty") int qty);
}
