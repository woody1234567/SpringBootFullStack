package com.example.expensetracker.controller;

import com.example.expensetracker.dto.response.ApiResponse;
import com.example.expensetracker.dto.response.ImportResultResponse;
import com.example.expensetracker.security.AuthenticatedUser;
import com.example.expensetracker.service.ExpenseImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ExpenseImportController {

    private final ExpenseImportService expenseImportService;

    @PostMapping(value = "/expenses", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ImportResultResponse>> importExpenses(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file
    ) {
        ImportResultResponse response = expenseImportService.importExpenses(user.userId(), file);
        String message = "SUCCESS".equals(response.status())
                ? "Import completed successfully"
                : "Import failed validation; no rows were imported";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}
