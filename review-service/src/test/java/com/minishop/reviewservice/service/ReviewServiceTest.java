package com.minishop.reviewservice.service;

import com.minishop.reviewservice.document.Review;
import com.minishop.reviewservice.document.ReviewStatus;
import com.minishop.reviewservice.dto.request.CreateReviewRequest;
import com.minishop.reviewservice.dto.response.ProductRatingSummaryResponse;
import com.minishop.reviewservice.dto.response.ReviewResponse;
import com.minishop.reviewservice.event.dto.ProductRatingUpdatedEvent;
import com.minishop.reviewservice.event.producer.ProductRatingUpdatedEventProducer;
import com.minishop.reviewservice.mapper.ReviewMapper;
import com.minishop.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewEligibilityChecker eligibilityChecker;

    @Mock
    private ProductRatingUpdatedEventProducer ratingEventProducer;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private UUID userId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID productId;
    private Review sampleReview;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
        productId = UUID.randomUUID();

        sampleReview = Review.builder()
                .id("rev-123")
                .orderId(orderId)
                .orderItemId(orderItemId)
                .productId(productId)
                .userId(userId)
                .userName("Nguyễn Văn A")
                .rating(5)
                .comment("Hàng rất đẹp")
                .status(ReviewStatus.VISIBLE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testCreateReviewSuccess() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .rating(5)
                .comment("Hàng rất đẹp")
                .images(List.of("https://cdn.example.com/img1.jpg"))
                .build();

        when(eligibilityChecker.checkEligibility(userId, orderId, orderItemId))
                .thenReturn(new ReviewEligibilityChecker.EligibilityResult(productId, "Nguyễn Văn A"));

        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);

        ProductRatingSummaryResponse summary = ProductRatingSummaryResponse.builder()
                .productId(productId)
                .avgRating(5.0)
                .totalReviews(1L)
                .ratingBreakdown(Map.of(5, 1L))
                .build();
        when(reviewRepository.getProductRatingSummary(productId)).thenReturn(summary);

        ReviewResponse responseDto = ReviewResponse.builder()
                .id("rev-123")
                .productId(productId)
                .rating(5)
                .comment("Hàng rất đẹp")
                .build();
        when(reviewMapper.toReviewResponse(sampleReview)).thenReturn(responseDto);

        ReviewResponse response = reviewService.createReview(userId, "Nguyễn Văn A", request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        verify(reviewRepository).save(any(Review.class));
        verify(ratingEventProducer).publishProductRatingUpdated(any(ProductRatingUpdatedEvent.class));
    }

    @Test
    void testReplyToReviewSuccess() {
        String reviewId = "rev-123";
        UUID sellerId = UUID.randomUUID();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse responseDto = ReviewResponse.builder()
                .id(reviewId)
                .build();
        when(reviewMapper.toReviewResponse(any(Review.class))).thenReturn(responseDto);

        ReviewResponse response = reviewService.replyToReview(reviewId, sellerId, "Cảm ơn bạn đã ủng hộ shop!");

        assertNotNull(response);
        verify(reviewRepository).save(sampleReview);
        assertNotNull(sampleReview.getSellerReply());
        assertEquals("Cảm ơn bạn đã ủng hộ shop!", sampleReview.getSellerReply().getContent());
    }

    @Test
    void testHideReviewSoftHidesAndRecalculatesRating() {
        String reviewId = "rev-123";

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRatingSummaryResponse summary = ProductRatingSummaryResponse.builder()
                .productId(productId)
                .avgRating(0.0)
                .totalReviews(0L)
                .build();
        when(reviewRepository.getProductRatingSummary(productId)).thenReturn(summary);

        ReviewResponse responseDto = ReviewResponse.builder()
                .id(reviewId)
                .status(ReviewStatus.HIDDEN)
                .build();
        when(reviewMapper.toReviewResponse(any(Review.class))).thenReturn(responseDto);

        ReviewResponse response = reviewService.hideReview(reviewId);

        assertNotNull(response);
        assertEquals(ReviewStatus.HIDDEN, sampleReview.getStatus());
        verify(ratingEventProducer).publishProductRatingUpdated(any(ProductRatingUpdatedEvent.class));
    }
}
