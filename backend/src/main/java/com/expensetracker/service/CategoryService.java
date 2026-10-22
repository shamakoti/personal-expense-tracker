package com.expensetracker.service;

import com.expensetracker.dto.CategoryRequest;
import com.expensetracker.dto.CategoryResponse;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.exception.ConflictException;
import com.expensetracker.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(TransactionType type) {
        return categoryRepository.findAll(Sort.by("type", "name")).stream()
                .filter(category -> type == null || category.getType() == type)
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndType(name, request.type())) {
            throw new ConflictException(
                    "A " + request.type() + " category named '" + name + "' already exists");
        }

        Category category = new Category(name, request.type(), false);
        return CategoryResponse.from(categoryRepository.save(category));
    }
}