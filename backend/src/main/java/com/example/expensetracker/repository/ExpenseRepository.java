package com.example.expensetracker.repository;

import com.example.expensetracker.repository.model.CreateExpenseResult;
import com.example.expensetracker.repository.model.GetExpenseResult;
import com.example.expensetracker.repository.model.MutationResult;
import com.example.expensetracker.repository.model.SearchExpensesResult;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseRepository {

    CreateExpenseResult createExpense(
            String userId, LocalDate expenseDate, BigDecimal amount, String categoryId,
            String invoiceNumber, String note);

    MutationResult updateExpense(
            String expenseId, String userId, LocalDate expenseDate, BigDecimal amount, String categoryId,
            String invoiceNumber, String note);

    MutationResult deleteExpense(String expenseId, String userId);

    GetExpenseResult getExpenseDetail(String expenseId, String userId);

    SearchExpensesResult searchExpenses(
            String userId, LocalDate dateFrom, LocalDate dateTo, String categoryId, int page, int pageSize);
}
