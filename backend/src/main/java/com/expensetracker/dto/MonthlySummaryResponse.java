package com.expensetracker.dto;

import java.math.BigDecimal;

public record MonthlySummaryResponse(
        String month,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal savings
) {
}