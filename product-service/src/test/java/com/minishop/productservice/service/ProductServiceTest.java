package com.minishop.productservice.service;

import com.minishop.productservice.dto.request.CreateProductRequest;
import com.minishop.productservice.dto.request.UpdateProductRequest;
import com.minishop.productservice.dto.response.ProductResponse;
import com.minishop.productservice.entity.Category;
import com.minishop.productservice.entity.Product;
import com.minishop.productservice.entity.ProductStatus;
import com.minishop.productservice.exception.ProductNotFoundException;
import com.minishop.productservice.exception.UnauthorizedProductAccessException;
import com.minishop.productservice.mapper.ProductMapper;
import com.minishop.productservice.repository.ProductImageRepository;
import com.minishop.productservice.repository.ProductRepository;
import com.minishop.productservice.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private UUID sellerId;
    private Category sampleCategory;
    private Product sampleProduct;
    private ProductResponse sampleProductResponse;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(sellerId)
                .email("seller@example.com")
                .role("SELLER")
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        sampleCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Thời trang nam")
                .slug("thoi-trang-nam")
                .build();

        sampleProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Áo thun basic")
                .slug("ao-thun-basic")
                .description("Chất liệu cotton 100%")
                .price(new BigDecimal("199000"))
                .stockQuantity(50)
                .category(sampleCategory)
                .sellerId(sellerId)
                .status(ProductStatus.ACTIVE)
                .avgRating(BigDecimal.ZERO)
                .images(new ArrayList<>())
                .build();

        sampleProductResponse = ProductResponse.builder()
                .id(sampleProduct.getId())
                .name(sampleProduct.getName())
                .slug(sampleProduct.getSlug())
                .price(sampleProduct.getPrice())
                .stockQuantity(sampleProduct.getStockQuantity())
                .sellerId(sellerId)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateProductSuccess() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Áo thun basic")
                .description("Chất liệu cotton 100%")
                .price(new BigDecimal("199000"))
                .stockQuantity(50)
                .categoryId(sampleCategory.getId())
                .images(Collections.singletonList("https://example.com/image.jpg"))
                .build();

        when(categoryService.getCategoryEntityById(sampleCategory.getId())).thenReturn(sampleCategory);
        when(productRepository.existsBySlug("ao-thun-basic")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toProductResponse(sampleProduct)).thenReturn(sampleProductResponse);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("Áo thun basic", response.getName());
        assertEquals(sellerId, response.getSellerId());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testGetProductByIdSuccess() {
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));
        when(productMapper.toProductResponse(sampleProduct)).thenReturn(sampleProductResponse);

        ProductResponse response = productService.getProductById(sampleProduct.getId());

        assertNotNull(response);
        assertEquals(sampleProduct.getId(), response.getId());
    }

    @Test
    void testGetProductByIdHiddenThrowsException() {
        sampleProduct.setStatus(ProductStatus.HIDDEN);
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(sampleProduct.getId()));
    }

    @Test
    void testUpdateProductUnauthorizedThrowsException() {
        // Change sellerId to simulate different owner
        sampleProduct.setSellerId(UUID.randomUUID());
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Áo thun updated")
                .build();

        assertThrows(UnauthorizedProductAccessException.class,
                () -> productService.updateProduct(sampleProduct.getId(), request));
    }

    @Test
    void testSoftDeleteProductSuccess() {
        when(productRepository.findById(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        productService.deleteProduct(sampleProduct.getId());

        assertEquals(ProductStatus.HIDDEN, sampleProduct.getStatus());
        verify(productRepository).save(sampleProduct);
    }
}
