package com.expensetracker.controller;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// TEMPORARY: used only to test Phase 3. Delete in Phase 4.
@RestController
@RequestMapping("/api/dev")
public class DevController {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public DevController(CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return categoryRepository.findAll();
    }

    @PostMapping("/sample")
    public Map<String, Object> createSample() {
        Category food = categoryRepository.findByType(TransactionType.EXPENSE).stream()
                .filter(c -> c.getName().equals("Food"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Food category not found"));

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("250.50"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setCategory(food);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setDescription("Sample lunch");

        Transaction saved = transactionRepository.save(transaction);

        return Map.of(
                "id", saved.getId(),
                "amount", saved.getAmount(),
                "category", food.getName()
        );
    }

    @GetMapping("/transactions/count")
    public Map<String, Long> transactionCount() {
        return Map.of("count", transactionRepository.count());
    }
}