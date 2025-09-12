package com.minishop.productservice.service;

import com.minishop.productservice.dto.request.CreateCategoryRequest;
import com.minishop.productservice.dto.response.CategoryResponse;
import com.minishop.productservice.entity.Category;
import com.minishop.productservice.exception.CategoryNotFoundException;
import com.minishop.productservice.mapper.CategoryMapper;
import com.minishop.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNull();
        return categoryMapper.toCategoryResponseList(rootCategories);
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntityById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + id));
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = getCategoryEntityById(request.getParentId());
        }

        String baseSlug = toSlug(request.getName());
        String slug = baseSlug;
        int counter = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .parent(parent)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
