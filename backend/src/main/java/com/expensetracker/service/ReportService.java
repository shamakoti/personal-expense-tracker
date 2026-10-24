package com.expensetracker.service;

import com.expensetracker.dto.CategoryTotalResponse;
import com.expensetracker.dto.CategoryTotalRow;
import com.expensetracker.dto.MonthlySummaryResponse;
import com.expensetracker.dto.MonthlyTotalRow;
import com.expensetracker.dto.SummaryResponse;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Income, expenses and savings for one month, or for all time when month is null. */
    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long userId, String month) {
        DateRange range = DateRange.ofMonth(month);
        BigDecimal income = total(userId, TransactionType.INCOME, range);
        BigDecimal expenses = total(userId, TransactionType.EXPENSE, range);
        return new SummaryResponse(income, expenses, calculateSavings(income, expenses));
    }

    /** One entry per month that has transactions, oldest first. */
    @Transactional(readOnly = true)
    public List<MonthlySummaryResponse> getMonthlySummaries(Long userId) {
        Map<YearMonth, BigDecimal> incomeByMonth = new TreeMap<>();
        Map<YearMonth, BigDecimal> expensesByMonth = new TreeMap<>();

        for (MonthlyTotalRow row : transactionRepository.findMonthlyTotals(userId)) {
            YearMonth month = YearMonth.of(row.year(), row.month());
            if (row.type() == TransactionType.INCOME) {
                incomeByMonth.put(month, row.total());
            } else {
                expensesByMonth.put(month, row.total());
            }
        }

        Set<YearMonth> months = new TreeSet<>(incomeByMonth.keySet());
        months.addAll(expensesByMonth.keySet());

        return months.stream()
                .map(month -> {
                    BigDecimal income = money(incomeByMonth.get(month));
                    BigDecimal expenses = money(expensesByMonth.get(month));
                    return new MonthlySummaryResponse(
                            month.toString(), income, expenses, calculateSavings(income, expenses));
                })
                .toList();
    }

    /** Total per category with each category's share of the whole, biggest first. */
    @Transactional(readOnly = true)
    public List<CategoryTotalResponse> getCategoryTotals(Long userId, String month, TransactionType type) {
        DateRange range = DateRange.ofMonth(month);
        TransactionType effectiveType = (type == null) ? TransactionType.EXPENSE : type;

        List<CategoryTotalRow> rows =
                transactionRepository.findCategoryTotals(userId, effectiveType, range.from(), range.to());

        BigDecimal grandTotal = rows.stream()
                .map(CategoryTotalRow::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> new CategoryTotalResponse(
                        row.categoryId(),
                        row.categoryName(),
                        money(row.total()),
                        calculatePercentage(row.total(), grandTotal)))
                .toList();
    }

    // ----- Pure calculations (public static so they are easy to unit test in Phase 10) -----

    public static BigDecimal calculateSavings(BigDecimal income, BigDecimal expenses) {
        return income.subtract(expenses);
    }

    public static BigDecimal calculatePercentage(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal total(Long userId, TransactionType type, DateRange range) {
        return money(transactionRepository.sumAmount(userId, type, range.from(), range.to()));
    }

    // Treats "no rows" (null) as zero and always uses two decimal places
    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}