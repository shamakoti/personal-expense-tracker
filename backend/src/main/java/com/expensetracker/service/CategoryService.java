package com.expensetracker.service;

import com.expensetracker.dto.CategoryResponse;
import com.expensetracker.entity.TransactionType;
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
}