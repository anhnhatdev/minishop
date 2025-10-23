package com.minishop.reviewservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "reviews")
@CompoundIndex(def = "{'productId': 1, 'createdAt': -1}", name = "idx_product_created")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    private String id;

    @Indexed
    private UUID orderId;

    @Indexed(unique = true)
    private UUID orderItemId;

    @Indexed
    private UUID productId;

    @Indexed
    private UUID userId;

    private String userName;

    private Integer rating;

    private String comment;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    private SellerReply sellerReply;

    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
