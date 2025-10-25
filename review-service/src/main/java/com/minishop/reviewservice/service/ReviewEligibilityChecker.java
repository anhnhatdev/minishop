package com.minishop.reviewservice.service;

import com.minishop.reviewservice.client.OrderServiceClient;
import com.minishop.reviewservice.client.dto.OrderDto;
import com.minishop.reviewservice.client.dto.OrderItemDto;
import com.minishop.reviewservice.exception.DuplicateReviewException;
import com.minishop.reviewservice.exception.ReviewNotEligibleException;
import com.minishop.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEligibilityChecker {

    private final OrderServiceClient orderServiceClient;
    private final ReviewRepository reviewRepository;

    public EligibilityResult checkEligibility(UUID userId, UUID orderId, UUID orderItemId) {
        log.info("Checking review eligibility for user: {}, order: {}, item: {}", userId, orderId, orderItemId);

        // 1. Check if already reviewed
        if (reviewRepository.existsByOrderItemId(orderItemId)) {
            throw new DuplicateReviewException("Order item " + orderItemId + " has already been reviewed");
        }

        // 2. Fetch order via Feign
        OrderDto order;
        try {
            order = orderServiceClient.getOrderById(orderId);
        } catch (Exception ex) {
            log.error("Failed to fetch order {} from order-service: {}", orderId, ex.getMessage());
            throw new ReviewNotEligibleException("Unable to verify order with order-service: " + ex.getMessage());
        }

        if (order == null) {
            throw new ReviewNotEligibleException("Order not found: " + orderId);
        }

        // 3. Check ownership
        if (!userId.equals(order.getUserId())) {
            throw new ReviewNotEligibleException("You are not authorized to review an order that does not belong to you");
        }

        // 4. Check DELIVERED status
        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new ReviewNotEligibleException("Reviews are only allowed after the order has been DELIVERED. Current status: " + order.getStatus());
        }

        // 5. Check order item belongs to order
        OrderItemDto matchingItem = order.getItems().stream()
                .filter(item -> orderItemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ReviewNotEligibleException("Order item " + orderItemId + " does not belong to order " + orderId));

        return new EligibilityResult(matchingItem.getProductId(), order.getCustomerName());
    }

    public record EligibilityResult(UUID productId, String customerName) {}
}
