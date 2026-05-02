package com.omnishop.productservice.service;

import com.omnishop.productservice.dto.*;
import com.omnishop.productservice.entity.Product;
import com.omnishop.productservice.exception.AccessDeniedException;
import com.omnishop.productservice.exception.ProductNotFoundException;
import com.omnishop.productservice.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final Counter productsCreated;
    private final Counter productsViewed;

    public ProductService(ProductRepository productRepository, MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.productsCreated = Counter.builder("shop.products.create")
                .description("Total products created by sellers")
                .register(meterRegistry);
        this.productsViewed = Counter.builder("products.viewed")
                .description("Total product detail page views")
                .register(meterRegistry);
    }

    public Page<ProductSummaryDTO> getAll(int page, int size, String search) {
        int safePage = Math.min(Math.max(page, 0), 10000);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Product> products = (search != null && !search.isBlank())
                ? productRepository.findByNameContainingIgnoreCase(search, pageable)
                : productRepository.findAll(pageable);

        return products.map(this::toSummary);
    }

    public ProductDetailDTO getById(UUID id) {
        productsViewed.increment();
        return toDetail(findActive(id));
    }

    @Transactional
    public ProductDetailDTO create(ProductRequest request, UUID sellerId) {
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        productsCreated.increment();
        return toDetail(productRepository.save(product));
    }

    @Transactional
    public ProductDetailDTO update(UUID id, ProductUpdateRequest request, UUID userId) {
        Product product = findActive(id);
        checkOwnership(product, userId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        return toDetail(productRepository.save(product));
    }

    @Transactional
    public void softDelete(UUID id, UUID userId) {
        Product product = findActive(id);
        checkOwnership(product, userId);
        product.setActive(false);
        productRepository.save(product);
    }

    public ProductDetailDTO getQuantity(UUID id, UUID userId) {
        Product product = findActive(id);
        checkOwnership(product, userId);
        return toDetail(product);
    }

    @Transactional
    public ProductDetailDTO setQuantity(UUID id, QuantityUpdateRequest request, UUID userId) {
        Product product = findActive(id);
        checkOwnership(product, userId);
        product.setQuantity(request.getQuantity());
        return toDetail(productRepository.save(product));
    }

    @Transactional
    public ProductDetailDTO addQuantity(UUID id, QuantityAddRequest request, UUID userId) {
        Product product = findActive(id);
        checkOwnership(product, userId);
        product.setQuantity(product.getQuantity() + request.getQuantity());
        return toDetail(productRepository.save(product));
    }

    private Product findActive(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
    }

    private void checkOwnership(Product product, UUID userId) {
        if (!product.getSellerId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to access this product");
        }
    }

    private ProductSummaryDTO toSummary(Product p) {
        String desc = p.getDescription();
        String preview = desc != null ? desc.substring(0, Math.min(100, desc.length())) : "";
        return new ProductSummaryDTO(p.getId(), p.getName(), p.getPrice(), preview);
    }

    private ProductDetailDTO toDetail(Product p) {
        return new ProductDetailDTO(
                p.getId(),
                p.getSellerId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getQuantity(),
                p.getCreatedAt()
        );
    }
}
