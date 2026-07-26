package com.example.expensetracker.repository.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseRow(
        String expenseId,
        LocalDate expenseDate,
        BigDecimal amount,
        String categoryId,
        String categoryName,
        String invoiceNumber,
        String note,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
