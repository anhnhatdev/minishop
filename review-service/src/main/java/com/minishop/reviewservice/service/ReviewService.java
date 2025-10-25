package com.minishop.reviewservice.service;

import com.minishop.reviewservice.document.Review;
import com.minishop.reviewservice.document.ReviewStatus;
import com.minishop.reviewservice.document.SellerReply;
import com.minishop.reviewservice.dto.request.CreateReviewRequest;
import com.minishop.reviewservice.dto.response.PagedResponse;
import com.minishop.reviewservice.dto.response.ProductRatingSummaryResponse;
import com.minishop.reviewservice.dto.response.ReviewResponse;
import com.minishop.reviewservice.event.dto.ProductRatingUpdatedEvent;
import com.minishop.reviewservice.event.producer.ProductRatingUpdatedEventProducer;
import com.minishop.reviewservice.exception.ReviewNotFoundException;
import com.minishop.reviewservice.mapper.ReviewMapper;
import com.minishop.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewEligibilityChecker eligibilityChecker;
    private final ProductRatingUpdatedEventProducer ratingEventProducer;
    private final ReviewMapper reviewMapper;

    public ReviewResponse createReview(UUID userId, String userName, CreateReviewRequest request) {
        log.info("Creating review for user: {}, orderItem: {}", userId, request.getOrderItemId());

        ReviewEligibilityChecker.EligibilityResult eligibility =
                eligibilityChecker.checkEligibility(userId, request.getOrderId(), request.getOrderItemId());

        String customerName = (userName != null && !userName.isBlank()) ? userName : eligibility.customerName();

        Review review = Review.builder()
                .orderId(request.getOrderId())
                .orderItemId(request.getOrderItemId())
                .productId(eligibility.productId())
                .userId(userId)
                .userName(customerName != null ? customerName : "Khách hàng")
                .rating(request.getRating())
                .comment(request.getComment())
                .images(request.getImages() != null ? request.getImages() : java.util.Collections.emptyList())
                .status(ReviewStatus.VISIBLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Compute updated rating summary via Mongo Aggregation
        syncProductRating(eligibility.productId());

        return reviewMapper.toReviewResponse(savedReview);
    }

    public PagedResponse<ReviewResponse> getReviewsByProduct(UUID productId, Integer rating, Pageable pageable) {
        Page<Review> reviewPage;
        if (rating != null) {
            reviewPage = reviewRepository.findByProductIdAndRatingAndStatus(productId, rating, ReviewStatus.VISIBLE, pageable);
        } else {
            reviewPage = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.VISIBLE, pageable);
        }

        return PagedResponse.<ReviewResponse>builder()
                .content(reviewMapper.toReviewResponseList(reviewPage.getContent()))
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    public ProductRatingSummaryResponse getProductRatingSummary(UUID productId) {
        return reviewRepository.getProductRatingSummary(productId);
    }

    public ReviewResponse replyToReview(String reviewId, UUID sellerId, String replyContent) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found for ID: " + reviewId));

        SellerReply reply = SellerReply.builder()
                .sellerId(sellerId)
                .content(replyContent)
                .repliedAt(Instant.now())
                .build();

        review.setSellerReply(reply);
        review.setUpdatedAt(Instant.now());

        Review saved = reviewRepository.save(review);
        return reviewMapper.toReviewResponse(saved);
    }

    public ReviewResponse hideReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found for ID: " + reviewId));

        review.setStatus(ReviewStatus.HIDDEN);
        review.setUpdatedAt(Instant.now());

        Review saved = reviewRepository.save(review);

        // Recalculate rating excluding hidden review
        syncProductRating(saved.getProductId());

        return reviewMapper.toReviewResponse(saved);
    }

    private void syncProductRating(UUID productId) {
        try {
            ProductRatingSummaryResponse summary = reviewRepository.getProductRatingSummary(productId);
            ProductRatingUpdatedEvent event = ProductRatingUpdatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("product.rating.updated")
                    .productId(productId)
                    .newAvgRating(summary.getAvgRating())
                    .newTotalReviews(summary.getTotalReviews())
                    .timestamp(Instant.now())
                    .build();

            ratingEventProducer.publishProductRatingUpdated(event);
        } catch (Exception ex) {
            log.error("Failed to calculate and sync product rating for {}: {}", productId, ex.getMessage());
        }
    }
}
