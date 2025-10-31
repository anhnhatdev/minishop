package com.minishop.productservice.event.consumer;

import com.minishop.productservice.entity.Product;
import com.minishop.productservice.event.dto.InventoryUpdatedEvent;
import com.minishop.productservice.event.dto.ProductRatingUpdatedEvent;
import com.minishop.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSyncConsumer {

    private final ProductRepository productRepository;

    @KafkaListener(topics = "inventory.updated", groupId = "product-service-group")
    @Transactional
    public void handleInventoryUpdated(InventoryUpdatedEvent event) {
        log.info("Received inventory.updated for productId: {}, availableQty: {}", event.getProductId(), event.getAvailableQuantity());
        if (event.getProductId() == null || event.getAvailableQuantity() == null) {
            return;
        }

        Optional<Product> productOpt = productRepository.findById(event.getProductId());
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStockQuantity(event.getAvailableQuantity());
            productRepository.save(product);
            log.info("Successfully updated stockQuantity to {} for product {}", event.getAvailableQuantity(), product.getId());
        } else {
            log.warn("Product not found for inventory.updated event: {}", event.getProductId());
        }
    }

    @KafkaListener(topics = "product.rating.updated", groupId = "product-service-group")
    @Transactional
    public void handleProductRatingUpdated(ProductRatingUpdatedEvent event) {
        log.info("Received product.rating.updated for productId: {}, avgRating: {}, totalReviews: {}",
                event.getProductId(), event.getNewAvgRating(), event.getNewTotalReviews());
        if (event.getProductId() == null || event.getNewAvgRating() == null) {
            return;
        }

        Optional<Product> productOpt = productRepository.findById(event.getProductId());
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setAvgRating(BigDecimal.valueOf(event.getNewAvgRating()));
            productRepository.save(product);
            log.info("Successfully updated avgRating to {} for product {}", event.getNewAvgRating(), product.getId());
        } else {
            log.warn("Product not found for product.rating.updated event: {}", event.getProductId());
        }
    }
}
