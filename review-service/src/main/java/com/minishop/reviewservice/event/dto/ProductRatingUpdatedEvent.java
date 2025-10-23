package com.minishop.reviewservice.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRatingUpdatedEvent {

    private String eventId;
    private String eventType;
    private UUID productId;
    private Double newAvgRating;
    private Long newTotalReviews;
    private Instant timestamp;
}
