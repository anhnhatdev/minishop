package com.minishop.reviewservice.dto.response;

import com.minishop.reviewservice.document.ReviewStatus;
import com.minishop.reviewservice.document.SellerReply;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private String id;
    private UUID orderId;
    private UUID orderItemId;
    private UUID productId;
    private UUID userId;
    private String userName;
    private Integer rating;
    private String comment;
    private List<String> images;
    private SellerReply sellerReply;
    private ReviewStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
