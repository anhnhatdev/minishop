package com.minishop.reviewservice.repository;

import com.minishop.reviewservice.dto.response.ProductRatingSummaryResponse;

import java.util.UUID;

public interface ReviewRepositoryCustom {

    ProductRatingSummaryResponse getProductRatingSummary(UUID productId);
}
