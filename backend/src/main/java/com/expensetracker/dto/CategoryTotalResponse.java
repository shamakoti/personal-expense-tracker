package com.expensetracker.dto;

import java.math.BigDecimal;

public record CategoryTotalResponse(
        Long categoryId,
        String categoryName,
        BigDecimal total,
        BigDecimal percentage
) {
}