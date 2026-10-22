package com.expensetracker.controller;

import com.expensetracker.dto.CategoryTotalResponse;
import com.expensetracker.dto.MonthlySummaryResponse;
import com.expensetracker.dto.SummaryResponse;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.service.ReportService;
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
    public SummaryResponse summary(@RequestParam(name = "month", required = false) String month) {
        return reportService.getSummary(month);
    }

    @GetMapping("/monthly")
    public List<MonthlySummaryResponse> monthly() {
        return reportService.getMonthlySummaries();
    }

    @GetMapping("/categories")
    public List<CategoryTotalResponse> categories(
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "type", required = false) TransactionType type) {
        return reportService.getCategoryTotals(month, type);
    }
}