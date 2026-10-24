package com.expensetracker.controller;

import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.dto.TransactionResponse;
import com.expensetracker.dto.TransactionSearchCriteria;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.security.AppUserDetails;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public PageResponse<TransactionResponse> search(
            @AuthenticationPrincipal AppUserDetails user,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "type", required = false) TransactionType type,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "sort", defaultValue = "date-desc") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                search, type, categoryId, month, from, to, sort, page, size);
        return transactionService.search(user.getId(), criteria);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@AuthenticationPrincipal AppUserDetails user,
                                       @PathVariable("id") Long id) {
        return transactionService.getById(user.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal AppUserDetails user,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(user.getId(), request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal AppUserDetails user,
                                      @PathVariable("id") Long id,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails user,
                       @PathVariable("id") Long id) {
        transactionService.delete(user.getId(), id);
    }
}