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

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Search and filter. Any parameter that is null is ignored.
    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT t FROM Transaction t
            WHERE (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate IS NULL OR t.transactionDate <= :toDate)
              AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Transaction> search(@Param("search") String search,
                             @Param("type") TransactionType type,
                             @Param("categoryId") Long categoryId,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate,
                             Pageable pageable);

    // Total of one type, optionally limited to a date range
    @Query("""
            SELECT SUM(t.amount) FROM Transaction t
            WHERE t.type = :type
              AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate IS NULL OR t.transactionDate <= :toDate)
            """)
    BigDecimal sumAmount(@Param("type") TransactionType type,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate);

    // One row per (year, month, type) with the total amount
    @Query("""
            SELECT new com.expensetracker.dto.MonthlyTotalRow(
                YEAR(t.transactionDate), MONTH(t.transactionDate), t.type, SUM(t.amount))
            FROM Transaction t
            GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate), t.type
            """)
    List<MonthlyTotalRow> findMonthlyTotals();

    // One row per category with the total amount, biggest first
    @Query("""
            SELECT new com.expensetracker.dto.CategoryTotalRow(c.id, c.name, SUM(t.amount))
            FROM Transaction t JOIN t.category c
            WHERE t.type = :type
              AND (:fromDate IS NULL OR t.transactionDate >= :fromDate)
              AND (:toDate IS NULL OR t.transactionDate <= :toDate)
            GROUP BY c.id, c.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryTotalRow> findCategoryTotals(@Param("type") TransactionType type,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);
}