package com.expensetracker.dto;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.TransactionType;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        boolean defaultCategory
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.isDefaultCategory()
        );
    }
}