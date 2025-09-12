package com.minishop.productservice.dto.response;

import com.minishop.productservice.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private UUID categoryId;
    private String categoryName;
    private UUID sellerId;
    private ProductStatus status;
    private BigDecimal avgRating;
    private List<ProductImageResponse> images;
    private Instant createdAt;
    private Instant updatedAt;
}
