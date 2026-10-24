package com.expensetracker.dto;

public record CsrfResponse(String token, String headerName) {
}
