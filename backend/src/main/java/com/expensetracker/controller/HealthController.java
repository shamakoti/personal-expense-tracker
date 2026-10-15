package com.expensetracker.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "application", "Personal Expense Tracker"
        );
    }

    @GetMapping("/health/db")
    public Map<String, String> databaseHealth() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        return Map.of(
                "status", "UP",
                "database", databaseName
        );
    }
}