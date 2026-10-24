package com.expensetracker.util;

import com.expensetracker.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void aMonthCoversItsFirstAndLastDay() {
        DateRange range = DateRange.ofMonth("2026-10");

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void februaryInALeapYearHas29Days() {
        assertThat(DateRange.ofMonth("2028-02").to()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void aMissingMonthMeansNoLimit() {
        DateRange range = DateRange.ofMonth(null);

        assertThat(range.from()).isNull();
        assertThat(range.to()).isNull();
    }

    @Test
    void aBadMonthIsRejected() {
        assertThatThrownBy(() -> DateRange.ofMonth("2026-13"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("yyyy-MM");
    }

    @Test
    void aFromDateAfterTheToDateIsRejected() {
        assertThatThrownBy(() -> DateRange.of(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(InvalidRequestException.class);
    }
}