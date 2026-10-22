package com.expensetracker.dto;

import java.math.BigDecimal;

public record CategoryTotalRow(
        Long categoryId,
        String categoryName,
        BigDecimal total
) {
}