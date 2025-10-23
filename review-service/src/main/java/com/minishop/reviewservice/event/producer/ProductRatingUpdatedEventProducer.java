package com.minishop.reviewservice.event.producer;

import com.minishop.reviewservice.event.dto.ProductRatingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRatingUpdatedEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_PRODUCT_RATING_UPDATED = "product.rating.updated";

    public void publishProductRatingUpdated(ProductRatingUpdatedEvent event) {
        log.info("Publishing {} for productId: {}, newAvgRating: {}, totalReviews: {}",
                TOPIC_PRODUCT_RATING_UPDATED, event.getProductId(), event.getNewAvgRating(), event.getNewTotalReviews());
        kafkaTemplate.send(TOPIC_PRODUCT_RATING_UPDATED, event.getProductId().toString(), event);
    }
}
