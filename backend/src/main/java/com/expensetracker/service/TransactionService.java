package com.expensetracker.service;

import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.dto.TransactionResponse;
import com.expensetracker.dto.TransactionSearchCriteria;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.Transaction;
import com.expensetracker.exception.InvalidRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.DateRange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

@Service
public class TransactionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(TransactionSearchCriteria criteria) {
        DateRange range = (criteria.month() != null && !criteria.month().isBlank())
                ? DateRange.ofMonth(criteria.month())
                : DateRange.of(criteria.from(), criteria.to());

        int page = Math.max(criteria.page(), 0);
        int size = Math.min(Math.max(criteria.size(), 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, resolveSort(criteria.sort()));

        String search = (criteria.search() == null || criteria.search().isBlank())
                ? null
                : criteria.search().trim();

        Page<Transaction> result = transactionRepository.search(
                search, criteria.type(), criteria.categoryId(), range.from(), range.to(), pageable);

        return PageResponse.from(result.map(TransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        return TransactionResponse.from(findTransaction(id));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Category category = findAndCheckCategory(request);
        Transaction transaction = new Transaction();
        copyFields(transaction, request, category);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = findTransaction(id);
        Category category = findAndCheckCategory(request);
        copyFields(transaction, request, category);
        // saveAndFlush writes to the database now, so updatedAt is set before we build the response
        return TransactionResponse.from(transactionRepository.saveAndFlush(transaction));
    }

    @Transactional
    public void delete(Long id) {
        Transaction transaction = findTransaction(id);
        transactionRepository.delete(transaction);
    }

    // Only these exact sort options are allowed, so a client cannot sort by arbitrary fields
    private Sort resolveSort(String sort) {
        return switch (sort == null ? "date-desc" : sort) {
            case "date-desc" -> Sort.by(Sort.Direction.DESC, "transactionDate", "id");
            case "date-asc" -> Sort.by(Sort.Direction.ASC, "transactionDate", "id");
            case "amount-desc" -> Sort.by(Sort.Direction.DESC, "amount", "id");
            case "amount-asc" -> Sort.by(Sort.Direction.ASC, "amount", "id");
            default -> throw new InvalidRequestException(
                    "Invalid sort '" + sort + "'. Use date-desc, date-asc, amount-desc or amount-asc");
        };
    }

    private Transaction findTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id " + id));
    }

    private Category findAndCheckCategory(TransactionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id " + request.categoryId()));

        if (category.getType() != request.type()) {
            throw new InvalidRequestException(
                    "Category '" + category.getName() + "' is an " + category.getType()
                            + " category and cannot be used for an " + request.type()
                            + " transaction");
        }
        return category;
    }

    private void copyFields(Transaction transaction, TransactionRequest request, Category category) {
        transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        transaction.setType(request.type());
        transaction.setCategory(category);
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(
                request.description() == null ? null : request.description().trim());
    }
}