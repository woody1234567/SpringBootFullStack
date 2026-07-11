package com.example.expensetracker.repository.model;

import java.util.List;

public record SearchExpensesResult(
        List<ExpenseRow> expenses,
        int totalCount,
        String resultCode,
        String resultMessage
) {
}
