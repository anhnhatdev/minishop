package com.minishop.reviewservice.service;

import com.minishop.reviewservice.client.OrderServiceClient;
import com.minishop.reviewservice.client.dto.OrderDto;
import com.minishop.reviewservice.client.dto.OrderItemDto;
import com.minishop.reviewservice.exception.DuplicateReviewException;
import com.minishop.reviewservice.exception.ReviewNotEligibleException;
import com.minishop.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewEligibilityCheckerTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewEligibilityChecker eligibilityChecker;

    private UUID userId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void testAlreadyReviewedThrowsDuplicateException() {
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(true);

        assertThrows(DuplicateReviewException.class,
                () -> eligibilityChecker.checkEligibility(userId, orderId, orderItemId));
    }

    @Test
    void testOrderNotBelongingToUserThrowsNotEligible() {
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);

        OrderDto otherUserOrder = OrderDto.builder()
                .id(orderId)
                .userId(UUID.randomUUID()) // Different user
                .status("DELIVERED")
                .build();

        when(orderServiceClient.getOrderById(orderId)).thenReturn(otherUserOrder);

        assertThrows(ReviewNotEligibleException.class,
                () -> eligibilityChecker.checkEligibility(userId, orderId, orderItemId));
    }

    @Test
    void testOrderNotDeliveredTestThrowsNotEligible() {
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);

        OrderDto pendingOrder = OrderDto.builder()
                .id(orderId)
                .userId(userId)
                .status("SHIPPING") // Not DELIVERED
                .build();

        when(orderServiceClient.getOrderById(orderId)).thenReturn(pendingOrder);

        assertThrows(ReviewNotEligibleException.class,
                () -> eligibilityChecker.checkEligibility(userId, orderId, orderItemId));
    }

    @Test
    void testItemNotInOrderThrowsNotEligible() {
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);

        OrderDto deliveredOrder = OrderDto.builder()
                .id(orderId)
                .userId(userId)
                .status("DELIVERED")
                .items(List.of(
                        OrderItemDto.builder().id(UUID.randomUUID()).productId(productId).build()
                ))
                .build();

        when(orderServiceClient.getOrderById(orderId)).thenReturn(deliveredOrder);

        assertThrows(ReviewNotEligibleException.class,
                () -> eligibilityChecker.checkEligibility(userId, orderId, orderItemId));
    }

    @Test
    void testValidOrderReturnsEligibilityResult() {
        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);

        OrderItemDto item = OrderItemDto.builder()
                .id(orderItemId)
                .productId(productId)
                .productName("Áo Polo")
                .price(new BigDecimal("299000"))
                .quantity(1)
                .build();

        OrderDto deliveredOrder = OrderDto.builder()
                .id(orderId)
                .userId(userId)
                .customerName("Nguyễn Văn A")
                .status("DELIVERED")
                .items(List.of(item))
                .build();

        when(orderServiceClient.getOrderById(orderId)).thenReturn(deliveredOrder);

        ReviewEligibilityChecker.EligibilityResult result = eligibilityChecker.checkEligibility(userId, orderId, orderItemId);

        assertNotNull(result);
        assertEquals(productId, result.productId());
        assertEquals("Nguyễn Văn A", result.customerName());
    }
}
