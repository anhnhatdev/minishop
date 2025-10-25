package com.minishop.reviewservice.repository;

import com.minishop.reviewservice.document.Review;
import com.minishop.reviewservice.document.ReviewStatus;
import com.minishop.reviewservice.dto.response.ProductRatingSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public ProductRatingSummaryResponse getProductRatingSummary(UUID productId) {
        // Aggregation to group by rating to get count per star
        Aggregation breakdownAgg = newAggregation(
                match(Criteria.where("productId").is(productId).and("status").is(ReviewStatus.VISIBLE.name())),
                group("rating").count().as("count")
        );

        AggregationResults<Document> breakdownResults = mongoTemplate.aggregate(breakdownAgg, Review.class, Document.class);

        Map<Integer, Long> breakdown = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            breakdown.put(i, 0L);
        }

        long totalReviews = 0;
        double sumRating = 0;

        for (Document doc : breakdownResults.getMappedResults()) {
            Integer rating = doc.getInteger("_id");
            Number countNum = (Number) doc.get("count");
            long count = countNum != null ? countNum.longValue() : 0L;

            if (rating != null && rating >= 1 && rating <= 5) {
                breakdown.put(rating, count);
                totalReviews += count;
                sumRating += (rating * count);
            }
        }

        double avgRating = 0.0;
        if (totalReviews > 0) {
            avgRating = BigDecimal.valueOf(sumRating / totalReviews)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return ProductRatingSummaryResponse.builder()
                .productId(productId)
                .avgRating(avgRating)
                .totalReviews(totalReviews)
                .ratingBreakdown(breakdown)
                .build();
    }
}
