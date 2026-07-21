package com.example.expensetracker.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        String expenseId,
        LocalDate expenseDate,
        BigDecimal amount,
        String categoryId,
        String categoryName,
        String invoiceNumber,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
