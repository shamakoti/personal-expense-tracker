package com.expensetracker.service;

import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.entity.User;
import com.expensetracker.exception.InvalidRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionRequest request(TransactionType type, Long categoryId) {
        return new TransactionRequest(
                new BigDecimal("250.5"), type, categoryId, LocalDate.of(2026, 10, 15), "  Lunch  ");
    }

    @Test
    void createRejectsACategoryOfTheWrongType() {
        Category incomeCategory = new Category("Salary", TransactionType.INCOME, true);
        when(categoryRepository.findVisibleById(8L, USER_ID)).thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(() -> transactionService.create(USER_ID, request(TransactionType.EXPENSE, 8L)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot be used");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createRejectsACategoryTheUserCannotSee() {
        when(categoryRepository.findVisibleById(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(USER_ID, request(TransactionType.EXPENSE, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createStoresAnOwnedTransactionWithCleanedValues() {
        Category food = new Category("Food", TransactionType.EXPENSE, true);
        User owner = new User("asha@example.com", "Asha", "hash");
        when(categoryRepository.findVisibleById(1L, USER_ID)).thenReturn(Optional.of(food));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(call -> call.getArgument(0));

        transactionService.create(USER_ID, request(TransactionType.EXPENSE, 1L));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getAmount()).isEqualByComparingTo("250.50");
        assertThat(saved.getAmount().scale()).isEqualTo(2);
        assertThat(saved.getDescription()).isEqualTo("Lunch");
    }

    @Test
    void anotherUsersTransactionLooksLikeItDoesNotExist() {
        when(transactionRepository.findByIdAndUserId(40L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(USER_ID, 40L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aUserCannotDeleteAnotherUsersTransaction() {
        when(transactionRepository.findByIdAndUserId(40L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(USER_ID, 40L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
}