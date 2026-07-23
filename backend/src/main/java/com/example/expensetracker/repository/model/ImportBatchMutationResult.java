package com.example.expensetracker.repository.model;

public record ImportBatchMutationResult(
        String batchId,
        String resultCode,
        String resultMessage
) {
}
