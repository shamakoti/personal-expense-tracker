package com.expensetracker.service;

import com.expensetracker.dto.CategoryTotalResponse;
import com.expensetracker.dto.CategoryTotalRow;
import com.expensetracker.dto.MonthlySummaryResponse;
import com.expensetracker.dto.MonthlyTotalRow;
import com.expensetracker.dto.SummaryResponse;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    // ----- Pure calculations -----

    @Test
    void savingsAreIncomeMinusExpenses() {
        BigDecimal savings = ReportService.calculateSavings(
                new BigDecimal("52000.00"), new BigDecimal("12500.00"));

        assertThat(savings).isEqualByComparingTo("39500.00");
    }

    @Test
    void savingsCanBeNegative() {
        BigDecimal savings = ReportService.calculateSavings(
                new BigDecimal("1000.00"), new BigDecimal("1500.50"));

        assertThat(savings).isEqualByComparingTo("-500.50");
    }

    @Test
    void percentageIsRoundedToOneDecimalPlace() {
        BigDecimal percentage = ReportService.calculatePercentage(
                new BigDecimal("4800.00"), new BigDecimal("12500.00"));

        assertThat(percentage).isEqualByComparingTo("38.4");
    }

    @Test
    void percentageIsZeroWhenTheWholeIsZero() {
        BigDecimal percentage = ReportService.calculatePercentage(
                BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(percentage).isEqualByComparingTo("0");
    }

    // ----- Summary -----

    @Test
    void summaryCombinesIncomeAndExpenseTotals() {
        when(transactionRepository.sumAmount(USER_ID, TransactionType.INCOME, null, null))
                .thenReturn(new BigDecimal("164000.00"));
        when(transactionRepository.sumAmount(USER_ID, TransactionType.EXPENSE, null, null))
                .thenReturn(new BigDecimal("49501.50"));

        SummaryResponse summary = reportService.getSummary(USER_ID, null);

        assertThat(summary.totalIncome()).isEqualByComparingTo("164000.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("49501.50");
        assertThat(summary.savings()).isEqualByComparingTo("114498.50");
    }

    @Test
    void summaryTreatsMissingTotalsAsZero() {
        when(transactionRepository.sumAmount(USER_ID, TransactionType.INCOME, null, null))
                .thenReturn(null);
        when(transactionRepository.sumAmount(USER_ID, TransactionType.EXPENSE, null, null))
                .thenReturn(null);

        SummaryResponse summary = reportService.getSummary(USER_ID, null);

        assertThat(summary.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(summary.savings()).isEqualByComparingTo("0.00");
    }

    @Test
    void summaryForAMonthOnlyAsksForThatMonthsDates() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        when(transactionRepository.sumAmount(USER_ID, TransactionType.INCOME, start, end))
                .thenReturn(new BigDecimal("52000.00"));
        when(transactionRepository.sumAmount(USER_ID, TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("12500.00"));

        SummaryResponse summary = reportService.getSummary(USER_ID, "2026-10");

        assertThat(summary.savings()).isEqualByComparingTo("39500.00");
    }

    // ----- Monthly summaries -----

    @Test
    void monthlySummariesAreSortedAndPairIncomeWithExpenses() {
        when(transactionRepository.findMonthlyTotals(USER_ID)).thenReturn(List.of(
                new MonthlyTotalRow(2026, 10, TransactionType.EXPENSE, new BigDecimal("12500.00")),
                new MonthlyTotalRow(2026, 9, TransactionType.INCOME, new BigDecimal("62000.00")),
                new MonthlyTotalRow(2026, 9, TransactionType.EXPENSE, new BigDecimal("20401.00")),
                new MonthlyTotalRow(2026, 10, TransactionType.INCOME, new BigDecimal("52000.00"))));

        List<MonthlySummaryResponse> months = reportService.getMonthlySummaries(USER_ID);

        assertThat(months).extracting(MonthlySummaryResponse::month)
                .containsExactly("2026-09", "2026-10");
        assertThat(months.get(0).savings()).isEqualByComparingTo("41599.00");
        assertThat(months.get(1).savings()).isEqualByComparingTo("39500.00");
    }

    @Test
    void aMonthWithOnlyExpensesHasNegativeSavings() {
        when(transactionRepository.findMonthlyTotals(USER_ID)).thenReturn(List.of(
                new MonthlyTotalRow(2026, 8, TransactionType.EXPENSE, new BigDecimal("900.00"))));

        List<MonthlySummaryResponse> months = reportService.getMonthlySummaries(USER_ID);

        assertThat(months).hasSize(1);
        assertThat(months.get(0).income()).isEqualByComparingTo("0.00");
        assertThat(months.get(0).savings()).isEqualByComparingTo("-900.00");
    }

    // ----- Category totals -----

    @Test
    void categoryTotalsIncludeEachCategorysShare() {
        when(transactionRepository.findCategoryTotals(USER_ID, TransactionType.EXPENSE, null, null))
                .thenReturn(List.of(
                        new CategoryTotalRow(1L, "Food", new BigDecimal("4800.00")),
                        new CategoryTotalRow(4L, "Bills", new BigDecimal("3600.00")),
                        new CategoryTotalRow(2L, "Travel", new BigDecimal("3200.00")),
                        new CategoryTotalRow(7L, "Other", new BigDecimal("900.00"))));

        List<CategoryTotalResponse> totals = reportService.getCategoryTotals(USER_ID, null, null);

        assertThat(totals).extracting(CategoryTotalResponse::categoryName)
                .containsExactly("Food", "Bills", "Travel", "Other");
        assertThat(totals).extracting(CategoryTotalResponse::percentage)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(
                        new BigDecimal("38.4"),
                        new BigDecimal("28.8"),
                        new BigDecimal("25.6"),
                        new BigDecimal("7.2"));
    }
}