package com.example.expensetracker.repository.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single CSV data row after tolerant type coercion. Fields that could not be
 * parsed are left {@code null} so the stored procedure remains the single
 * source of truth for validation errors (see .claude/CLAUDE.md section 4).
 */
public record ParsedExpenseImportRow(
        int rowNumber,
        LocalDate expenseDate,
        BigDecimal amount,
        String categoryName,
        String invoiceNumber,
        String note
) {
}
