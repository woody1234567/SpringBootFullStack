package com.example.expensetracker.repository;

import com.example.expensetracker.repository.model.CreateExpenseResult;
import com.example.expensetracker.repository.model.GetExpenseResult;
import com.example.expensetracker.repository.model.MutationResult;
import com.example.expensetracker.repository.model.SearchExpensesResult;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseRepository {

    CreateExpenseResult createExpense(
            Long userId, LocalDate expenseDate, BigDecimal amount, Integer categoryId,
            String invoiceNumber, String note);

    MutationResult updateExpense(
            Long expenseId, Long userId, LocalDate expenseDate, BigDecimal amount, Integer categoryId,
            String invoiceNumber, String note);

    MutationResult deleteExpense(Long expenseId, Long userId);

    GetExpenseResult getExpenseDetail(Long expenseId, Long userId);

    SearchExpensesResult searchExpenses(
            Long userId, LocalDate dateFrom, LocalDate dateTo, Integer categoryId, int page, int pageSize);
}
