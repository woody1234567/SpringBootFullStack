package com.example.expensetracker.controller;

import com.example.expensetracker.dto.request.CreateExpenseRequest;
import com.example.expensetracker.dto.request.UpdateExpenseRequest;
import com.example.expensetracker.dto.response.ApiResponse;
import com.example.expensetracker.dto.response.ExpenseResponse;
import com.example.expensetracker.dto.response.PageResponse;
import com.example.expensetracker.security.AuthenticatedUser;
import com.example.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.createExpense(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", response));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long expenseId,
            @Valid @RequestBody UpdateExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.updateExpense(user.userId(), expenseId, request);
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", response));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long expenseId
    ) {
        expenseService.deleteExpense(user.userId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getDetail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long expenseId
    ) {
        ExpenseResponse response = expenseService.getExpense(user.userId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Expense retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> search(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        PageResponse<ExpenseResponse> response =
                expenseService.searchExpenses(user.userId(), dateFrom, dateTo, categoryId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully", response));
    }
}
