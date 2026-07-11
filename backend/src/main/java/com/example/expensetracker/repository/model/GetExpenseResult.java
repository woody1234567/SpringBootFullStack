package com.example.expensetracker.repository.model;

import java.util.Optional;

public record GetExpenseResult(Optional<ExpenseRow> expense, String resultCode, String resultMessage) {
}
