package com.expensetracker.util;

import com.expensetracker.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** A date range where either end may be null, which means "no limit". */
public record DateRange(LocalDate from, LocalDate to) {

    public static DateRange ofMonth(String month) {
        if (month == null || month.isBlank()) {
            return new DateRange(null, null);
        }
        try {
            YearMonth yearMonth = YearMonth.parse(month.trim());
            return new DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(
                    "Invalid month '" + month + "'. Use the format yyyy-MM, for example 2026-10");
        }
    }

    public static DateRange of(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("The 'from' date cannot be after the 'to' date");
        }
        return new DateRange(from, to);
    }
}