package com.minishop.reviewservice.controller;

import com.minishop.reviewservice.dto.request.CreateReviewRequest;
import com.minishop.reviewservice.dto.request.SellerReplyRequest;
import com.minishop.reviewservice.dto.response.PagedResponse;
import com.minishop.reviewservice.dto.response.ProductRatingSummaryResponse;
import com.minishop.reviewservice.dto.response.ReviewResponse;
import com.minishop.reviewservice.security.SecurityUtils;
import com.minishop.reviewservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Customer Reviews", description = "Endpoints for product ratings, customer comments, seller replies, and admin moderation")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Customer: Submit a review for a delivered order item")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUserName();
        ReviewResponse response = reviewService.createReview(userId, userName, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Public: Retrieve paginated reviews for a product with optional star rating filter")
    public ResponseEntity<PagedResponse<ReviewResponse>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer rating,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<ReviewResponse> response = reviewService.getReviewsByProduct(productId, rating, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}/summary")
    @Operation(summary = "Public: Get average rating and star breakdown summary for a product")
    public ResponseEntity<ProductRatingSummaryResponse> getProductRatingSummary(@PathVariable UUID productId) {
        ProductRatingSummaryResponse response = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Seller/Admin: Post a reply to a customer review")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable String id,
            @Valid @RequestBody SellerReplyRequest request
    ) {
        UUID sellerId = SecurityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.replyToReview(id, sellerId, request.getContent());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: Soft-hide an inappropriate or abusive review")
    public ResponseEntity<ReviewResponse> hideReview(@PathVariable String id) {
        ReviewResponse response = reviewService.hideReview(id);
        return ResponseEntity.ok(response);
    }
}
