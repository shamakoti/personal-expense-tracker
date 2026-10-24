package com.expensetracker.dto;

import com.expensetracker.entity.TransactionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // ----- TransactionRequest -----

    private TransactionRequest transaction(BigDecimal amount, String description) {
        return new TransactionRequest(
                amount, TransactionType.EXPENSE, 1L, LocalDate.of(2026, 10, 15), description);
    }

    private Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void aValidTransactionHasNoViolations() {
        assertThat(invalidFields(transaction(new BigDecimal("250.50"), "Lunch"))).isEmpty();
    }

    @Test
    void anAmountOfZeroIsRejected() {
        assertThat(invalidFields(transaction(BigDecimal.ZERO, "x"))).containsExactly("amount");
    }

    @Test
    void aNegativeAmountIsRejected() {
        assertThat(invalidFields(transaction(new BigDecimal("-5"), "x"))).containsExactly("amount");
    }

    @Test
    void moreThanTwoDecimalPlacesAreRejected() {
        assertThat(invalidFields(transaction(new BigDecimal("12.345"), "x"))).containsExactly("amount");
    }

    @Test
    void tooManyWholeDigitsAreRejected() {
        assertThat(invalidFields(transaction(new BigDecimal("12345678901.00"), "x")))
                .containsExactly("amount");
    }

    @Test
    void aTooLongDescriptionIsRejected() {
        assertThat(invalidFields(transaction(new BigDecimal("10.00"), "x".repeat(256))))
                .containsExactly("description");
    }

    @Test
    void missingFieldsAreAllReported() {
        TransactionRequest empty = new TransactionRequest(null, null, null, null, null);

        assertThat(invalidFields(empty))
                .containsExactlyInAnyOrder("amount", "type", "categoryId", "transactionDate");
    }

    // ----- RegisterRequest -----

    @Test
    void aValidRegistrationHasNoViolations() {
        assertThat(invalidFields(new RegisterRequest("Asha", "asha@example.com", "secret123"))).isEmpty();
    }

    @Test
    void anInvalidEmailIsRejected() {
        assertThat(invalidFields(new RegisterRequest("Asha", "not-an-email", "secret123")))
                .containsExactly("email");
    }

    @Test
    void aShortPasswordIsRejected() {
        assertThat(invalidFields(new RegisterRequest("Asha", "asha@example.com", "short")))
                .containsExactly("password");
    }

    @Test
    void aBlankNameIsRejected() {
        assertThat(invalidFields(new RegisterRequest("   ", "asha@example.com", "secret123")))
                .containsExactly("displayName");
    }

    // ----- CategoryRequest -----

    @Test
    void aBlankCategoryNameIsRejected() {
        assertThat(invalidFields(new CategoryRequest("  ", TransactionType.EXPENSE)))
                .containsExactly("name");
    }
}