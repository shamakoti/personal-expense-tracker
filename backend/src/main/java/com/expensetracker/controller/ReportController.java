package com.expensetracker.controller;

import com.expensetracker.dto.CategoryTotalResponse;
import com.expensetracker.dto.MonthlySummaryResponse;
import com.expensetracker.dto.SummaryResponse;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.security.AppUserDetails;
import com.expensetracker.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public SummaryResponse summary(@AuthenticationPrincipal AppUserDetails user,
                                   @RequestParam(name = "month", required = false) String month) {
        return reportService.getSummary(user.getId(), month);
    }

    @GetMapping("/monthly")
    public List<MonthlySummaryResponse> monthly(@AuthenticationPrincipal AppUserDetails user) {
        return reportService.getMonthlySummaries(user.getId());
    }

    @GetMapping("/categories")
    public List<CategoryTotalResponse> categories(
            @AuthenticationPrincipal AppUserDetails user,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "type", required = false) TransactionType type) {
        return reportService.getCategoryTotals(user.getId(), month, type);
    }
}