package com.example.expensetracker.repository.model;

import java.util.List;

public record ParsedExpenseImportBatch(
        List<ParsedExpenseImportRow> rows,
        List<ImportRowFailure> failedRows
) {
    public boolean hasFailures() {
        return failedRows != null && !failedRows.isEmpty();
    }
}
