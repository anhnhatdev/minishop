package com.minishop.productservice.event.consumer;

import com.minishop.productservice.entity.Product;
import com.minishop.productservice.event.dto.InventoryUpdatedEvent;
import com.minishop.productservice.event.dto.ProductRatingUpdatedEvent;
import com.minishop.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSyncConsumerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductSyncConsumer productSyncConsumer;

    private UUID productId;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        sampleProduct = Product.builder()
                .id(productId)
                .name("Test Phone")
                .slug("test-phone")
                .stockQuantity(10)
                .avgRating(BigDecimal.valueOf(4.0))
                .build();
    }

    @Test
    void testHandleInventoryUpdated() {
        InventoryUpdatedEvent event = InventoryUpdatedEvent.builder()
                .productId(productId)
                .availableQuantity(45)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productSyncConsumer.handleInventoryUpdated(event);

        assertEquals(45, sampleProduct.getStockQuantity());
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void testHandleProductRatingUpdated() {
        ProductRatingUpdatedEvent event = ProductRatingUpdatedEvent.builder()
                .productId(productId)
                .newAvgRating(4.8)
                .newTotalReviews(100L)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productSyncConsumer.handleProductRatingUpdated(event);

        assertEquals(BigDecimal.valueOf(4.8), sampleProduct.getAvgRating());
        verify(productRepository).save(sampleProduct);
    }
}
