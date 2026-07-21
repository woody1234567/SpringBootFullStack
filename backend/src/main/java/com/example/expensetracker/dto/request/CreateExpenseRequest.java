package com.example.expensetracker.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Category is required")
        @Pattern(regexp = "^[0-9A-F]{32}$", message = "Category ID must be a 32-character uppercase GUID")
        String categoryId,

        @Size(max = 20, message = "Invoice number must be at most 20 characters")
        String invoiceNumber,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}
