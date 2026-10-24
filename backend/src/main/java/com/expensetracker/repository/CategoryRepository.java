package com.expensetracker.repository;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Default categories (no owner) plus the user's own custom ones
    @Query("""
            SELECT c FROM Category c
            WHERE c.user IS NULL OR c.user.id = :userId
            ORDER BY c.type, c.name
            """)
    List<Category> findVisibleToUser(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Category c
            WHERE c.id = :id AND (c.user IS NULL OR c.user.id = :userId)
            """)
    Optional<Category> findVisibleById(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(c) FROM Category c
            WHERE LOWER(c.name) = LOWER(:name)
              AND c.type = :type
              AND (c.user IS NULL OR c.user.id = :userId)
            """)
    long countVisibleByNameAndType(@Param("userId") Long userId,
                                   @Param("name") String name,
                                   @Param("type") TransactionType type);
}