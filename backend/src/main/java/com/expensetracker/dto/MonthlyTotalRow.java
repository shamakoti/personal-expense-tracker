package com.expensetracker.dto;

import com.expensetracker.entity.TransactionType;

import java.math.BigDecimal;

public record MonthlyTotalRow(
        Integer year,
        Integer month,
        TransactionType type,
        BigDecimal total
) {
}