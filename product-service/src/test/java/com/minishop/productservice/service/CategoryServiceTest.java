package com.minishop.productservice.service;

import com.minishop.productservice.dto.request.CreateCategoryRequest;
import com.minishop.productservice.dto.response.CategoryResponse;
import com.minishop.productservice.entity.Category;
import com.minishop.productservice.exception.CategoryNotFoundException;
import com.minishop.productservice.mapper.CategoryMapper;
import com.minishop.productservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category sampleCategory;
    private CategoryResponse sampleCategoryResponse;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Thời trang nam")
                .slug("thoi-trang-nam")
                .build();

        sampleCategoryResponse = CategoryResponse.builder()
                .id(sampleCategory.getId())
                .name(sampleCategory.getName())
                .slug(sampleCategory.getSlug())
                .build();
    }

    @Test
    void testGetAllCategoriesTree() {
        when(categoryRepository.findByParentIsNull()).thenReturn(Collections.singletonList(sampleCategory));
        when(categoryMapper.toCategoryResponseList(any())).thenReturn(Collections.singletonList(sampleCategoryResponse));

        List<CategoryResponse> result = categoryService.getAllCategoriesTree();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Thời trang nam", result.getFirst().getName());
    }

    @Test
    void testCreateCategorySuccess() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Thời trang nam")
                .build();

        when(categoryRepository.existsBySlug("thoi-trang-nam")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);
        when(categoryMapper.toCategoryResponse(sampleCategory)).thenReturn(sampleCategoryResponse);

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("thoi-trang-nam", result.getSlug());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testGetCategoryByIdNotFoundThrowsException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryEntityById(id));
    }
}
