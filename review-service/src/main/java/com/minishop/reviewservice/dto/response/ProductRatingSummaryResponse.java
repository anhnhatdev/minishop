package com.minishop.reviewservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRatingSummaryResponse {

    private UUID productId;
    private Double avgRating;
    private Long totalReviews;
    @Builder.Default
    private Map<Integer, Long> ratingBreakdown = new HashMap<>();
}
