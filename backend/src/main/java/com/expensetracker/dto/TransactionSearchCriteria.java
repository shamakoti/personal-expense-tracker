package com.expensetracker.dto;

import com.expensetracker.entity.TransactionType;

import java.time.LocalDate;

public record TransactionSearchCriteria(
        String search,
        TransactionType type,
        Long categoryId,
        String month,
        LocalDate from,
        LocalDate to,
        String sort,
        int page,
        int size
) {
}