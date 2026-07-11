package com.example.expensetracker.repository.model;

import java.util.List;

public record ImportBatchResult(
        Long batchId,
        int successCount,
        String resultCode,
        String resultMessage,
        List<ImportRowFailure> failedRows
) {
}
