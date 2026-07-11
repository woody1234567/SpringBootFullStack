package com.example.expensetracker.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
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
