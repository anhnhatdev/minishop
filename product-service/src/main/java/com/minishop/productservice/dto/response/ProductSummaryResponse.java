package com.minishop.productservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private String thumbnailUrl;
    private BigDecimal avgRating;
    private String categoryName;
}
