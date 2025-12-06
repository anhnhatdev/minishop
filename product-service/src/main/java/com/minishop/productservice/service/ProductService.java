package com.minishop.productservice.service;

import com.minishop.productservice.dto.request.CreateProductRequest;
import com.minishop.productservice.dto.request.ProductFilterRequest;
import com.minishop.productservice.dto.request.UpdateProductRequest;
import com.minishop.productservice.dto.response.PagedResponse;
import com.minishop.productservice.dto.response.ProductResponse;
import com.minishop.productservice.dto.response.ProductSummaryResponse;
import com.minishop.productservice.entity.Category;
import com.minishop.productservice.entity.Product;
import com.minishop.productservice.entity.ProductImage;
import com.minishop.productservice.entity.ProductStatus;
import com.minishop.productservice.exception.ProductNotFoundException;
import com.minishop.productservice.exception.UnauthorizedProductAccessException;
import com.minishop.productservice.mapper.ProductMapper;
import com.minishop.productservice.repository.ProductImageRepository;
import com.minishop.productservice.repository.ProductRepository;
import com.minishop.productservice.repository.ProductSpecification;
import com.minishop.productservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryResponse> getProducts(ProductFilterRequest filter) {
        Pageable pageable = createPageable(filter.getPage(), filter.getSize(), filter.getSort());
        Page<Product> productPage = productRepository.findAll(ProductSpecification.filterBy(filter), pageable);

        List<ProductSummaryResponse> content = productPage.getContent().stream()
                .map(productMapper::toProductSummaryResponse)
                .toList();

        return PagedResponse.<ProductSummaryResponse>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new ProductNotFoundException("Product not found or has been removed");
        }

        return productMapper.toProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("User must be authenticated to create a product");
        }

        Category category = categoryService.getCategoryEntityById(request.getCategoryId());

        String baseSlug = CategoryService.toSlug(request.getName());
        String slug = baseSlug;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .category(category)
                .sellerId(currentUserId)
                .status(ProductStatus.ACTIVE)
                .avgRating(BigDecimal.ZERO)
                .images(new ArrayList<>())
                .build();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                String imageUrl = request.getImages().get(i);
                ProductImage image = ProductImage.builder()
                        .url(imageUrl)
                        .isThumbnail(i == 0) // First image is thumbnail by default
                        .displayOrder(i)
                        .build();
                product.addImage(image);
            }
        }

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        checkOwnership(product);

        if (StringUtils.hasText(request.getName())) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        // Note: stockQuantity is managed exclusively by inventory-service via inventory.updated event sync
        // to maintain append-only audit trail and prevent oversell race conditions.
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryEntityById(request.getCategoryId());
            product.setCategory(category);
        }

        if (request.getImages() != null) {
            product.getImages().clear();
            for (int i = 0; i < request.getImages().size(); i++) {
                String imageUrl = request.getImages().get(i);
                ProductImage image = ProductImage.builder()
                        .url(imageUrl)
                        .isThumbnail(i == 0)
                        .displayOrder(i)
                        .build();
                product.addImage(image);
            }
        }

        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        checkOwnership(product);

        // Soft delete
        product.setStatus(ProductStatus.HIDDEN);
        productRepository.save(product);
    }

    private void checkOwnership(Product product) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();

        if (currentUserId == null || (!isAdmin && !product.getSellerId().equals(currentUserId))) {
            throw new UnauthorizedProductAccessException("You are not authorized to modify or delete this product");
        }
    }

    private Pageable createPageable(Integer page, Integer size, String sortParam) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sortParam)) {
            String[] parts = sortParam.split(",");
            String property = parts[0].trim();
            Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            sort = Sort.by(direction, property);
        }

        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
