package com.minishop.productservice.mapper;

import com.minishop.productservice.dto.response.ProductImageResponse;
import com.minishop.productservice.dto.response.ProductResponse;
import com.minishop.productservice.dto.response.ProductSummaryResponse;
import com.minishop.productservice.entity.Product;
import com.minishop.productservice.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "thumbnailUrl", source = "images", qualifiedByName = "extractThumbnailUrl")
    ProductSummaryResponse toProductSummaryResponse(Product product);

    ProductImageResponse toProductImageResponse(ProductImage productImage);

    List<ProductImageResponse> toProductImageResponseList(List<ProductImage> productImages);

    @Named("extractThumbnailUrl")
    default String extractThumbnailUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(ProductImage::getIsThumbnail)
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(images.getFirst().getUrl());
    }
}
