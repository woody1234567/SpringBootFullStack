package com.example.expensetracker.service;

import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.dto.request.CreateExpenseRequest;
import com.example.expensetracker.dto.request.UpdateExpenseRequest;
import com.example.expensetracker.dto.response.ExpenseResponse;
import com.example.expensetracker.dto.response.PageResponse;
import com.example.expensetracker.exception.DuplicateResourceException;
import com.example.expensetracker.exception.ResourceNotFoundException;
import com.example.expensetracker.exception.ValidationException;
import com.example.expensetracker.repository.ExpenseRepository;
import com.example.expensetracker.repository.model.CreateExpenseResult;
import com.example.expensetracker.repository.model.ExpenseRow;
import com.example.expensetracker.repository.model.GetExpenseResult;
import com.example.expensetracker.repository.model.MutationResult;
import com.example.expensetracker.repository.model.SearchExpensesResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseResponse createExpense(Long userId, CreateExpenseRequest request) {
        CreateExpenseResult result = expenseRepository.createExpense(
                userId, request.expenseDate(), request.amount(), request.categoryId(),
                request.invoiceNumber(), request.note());

        applyMutationOutcome(result.resultCode(), result.resultMessage());
        return getExpense(userId, result.expenseId());
    }

    public ExpenseResponse updateExpense(Long userId, Long expenseId, UpdateExpenseRequest request) {
        MutationResult result = expenseRepository.updateExpense(
                expenseId, userId, request.expenseDate(), request.amount(), request.categoryId(),
                request.invoiceNumber(), request.note());

        applyMutationOutcome(result.resultCode(), result.resultMessage());
        return getExpense(userId, expenseId);
    }

    public void deleteExpense(Long userId, Long expenseId) {
        MutationResult result = expenseRepository.deleteExpense(expenseId, userId);
        applyMutationOutcome(result.resultCode(), result.resultMessage());
    }

    public ExpenseResponse getExpense(Long userId, Long expenseId) {
        GetExpenseResult result = expenseRepository.getExpenseDetail(expenseId, userId);

        ExpenseRow row = result.expense()
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return toResponse(row);
    }

    public PageResponse<ExpenseResponse> searchExpenses(
            Long userId, LocalDate dateFrom, LocalDate dateTo, Integer categoryId, int page, int pageSize
    ) {
        SearchExpensesResult result = expenseRepository.searchExpenses(userId, dateFrom, dateTo, categoryId, page, pageSize);

        if (!ResultCode.SUCCESS.equals(result.resultCode())) {
            log.error("app_expense.search_expenses returned result_code={}", result.resultCode());
            throw new IllegalStateException("Unable to complete the operation");
        }

        return new PageResponse<>(
                result.expenses().stream().map(this::toResponse).toList(),
                result.totalCount(),
                page,
                pageSize);
    }

    private void applyMutationOutcome(String resultCode, String resultMessage) {
        switch (resultCode) {
            case ResultCode.SUCCESS -> { /* no-op */ }
            case ResultCode.NOT_FOUND -> throw new ResourceNotFoundException("Expense not found");
            case ResultCode.DUPLICATE -> throw new DuplicateResourceException(resultMessage);
            case ResultCode.VALIDATION_ERROR -> throw new ValidationException(resultMessage);
            default -> {
                log.error("Unexpected result_code={} message={}", resultCode, resultMessage);
                throw new IllegalStateException("Unable to complete the operation");
            }
        }
    }

    private ExpenseResponse toResponse(ExpenseRow row) {
        return new ExpenseResponse(
                row.expenseId(), row.expenseDate(), row.amount(), row.categoryId(), row.categoryName(),
                row.invoiceNumber(), row.note(), row.createdAt(), row.updatedAt());
    }
}
