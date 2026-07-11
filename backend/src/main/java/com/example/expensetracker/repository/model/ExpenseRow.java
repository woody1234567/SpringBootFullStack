package com.example.expensetracker.repository.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseRow(
        Long expenseId,
        LocalDate expenseDate,
        BigDecimal amount,
        Integer categoryId,
        String categoryName,
        String invoiceNumber,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
