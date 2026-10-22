package com.expensetracker.dto;

import java.math.BigDecimal;

public record SummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal savings
) {
}