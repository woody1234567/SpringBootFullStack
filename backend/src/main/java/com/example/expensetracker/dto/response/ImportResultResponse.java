package com.example.expensetracker.dto.response;

import java.util.List;

public record ImportResultResponse(
        Long batchId,
        int totalRows,
        int successCount,
        String status,
        List<ImportRowError> failedRows
) {
}
