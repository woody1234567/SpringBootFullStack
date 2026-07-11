package com.example.expensetracker.repository.model;

public record CreateExpenseResult(Long expenseId, String resultCode, String resultMessage) {
}
