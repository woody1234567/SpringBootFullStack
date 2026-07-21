package com.example.expensetracker.repository.model;

import java.util.List;

public record ImportBatchResult(
        String batchId,
        int successCount,
        String resultCode,
        String resultMessage,
        List<ImportRowFailure> failedRows
) {
}
