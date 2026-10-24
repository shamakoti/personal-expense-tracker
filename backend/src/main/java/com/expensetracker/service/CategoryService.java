package com.expensetracker.service;

import com.expensetracker.dto.CategoryRequest;
import com.expensetracker.dto.CategoryResponse;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.exception.ConflictException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(Long userId, TransactionType type) {
        return categoryRepository.findVisibleToUser(userId).stream()
                .filter(category -> type == null || category.getType() == type)
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        String name = request.name().trim();

        if (categoryRepository.countVisibleByNameAndType(userId, name, request.type()) > 0) {
            throw new ConflictException(
                    "A " + request.type() + " category named '" + name + "' already exists");
        }

        Category category = new Category(name, request.type(), false);
        category.setUser(userRepository.getReferenceById(userId));
        return CategoryResponse.from(categoryRepository.save(category));
    }
}