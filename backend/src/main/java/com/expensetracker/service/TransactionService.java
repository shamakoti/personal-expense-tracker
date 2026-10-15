package com.expensetracker.service;

import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.dto.TransactionResponse;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.Transaction;
import com.expensetracker.exception.InvalidRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        return transactionRepository.findAllWithCategory().stream()
                .map(TransactionResponse::from)
                .toList();
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