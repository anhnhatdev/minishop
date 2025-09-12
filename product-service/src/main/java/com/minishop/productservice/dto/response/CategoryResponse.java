package com.minishop.productservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
    private List<CategoryResponse> subCategories;
    private Instant createdAt;
}
