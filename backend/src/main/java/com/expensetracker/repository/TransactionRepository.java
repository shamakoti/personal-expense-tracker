package com.expensetracker.repository;

import com.expensetracker.dto.CategoryTotalRow;
import com.expensetracker.dto.MonthlyTotalRow;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Finds a transaction only if it belongs to this user
    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate IS NULL OR t.transactionDate <= :toDate)
              AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Transaction> search(@Param("userId") Long userId,
                             @Param("search") String search,
                             @Param("type") TransactionType type,
                             @Param("categoryId") Long categoryId,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate,
                             Pageable pageable);

    @Query("""
            SELECT SUM(t.amount) FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = :type
              AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate IS NULL OR t.transactionDate <= :toDate)
            """)
    BigDecimal sumAmount(@Param("userId") Long userId,
                         @Param("type") TransactionType type,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT new com.expensetracker.dto.MonthlyTotalRow(
                YEAR(t.transactionDate), MONTH(t.transactionDate), t.type, SUM(t.amount))
            FROM Transaction t
            WHERE t.user.id = :userId
            GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate), t.type
            """)
    List<MonthlyTotalRow> findMonthlyTotals(@Param("userId") Long userId);

    @Query("""
            SELECT new com.expensetracker.dto.CategoryTotalRow(c.id, c.name, SUM(t.amount))
            FROM Transaction t JOIN t.category c
            WHERE t.user.id = :userId
              AND t.type = :type
              AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate IS NULL OR t.transactionDate <= :toDate)
            GROUP BY c.id, c.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryTotalRow> findCategoryTotals(@Param("userId") Long userId,
                                              @Param("type") TransactionType type,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);
}