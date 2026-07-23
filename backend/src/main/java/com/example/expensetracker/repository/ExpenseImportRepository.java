package com.example.expensetracker.repository;

import com.example.expensetracker.repository.model.ImportBatchMutationResult;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;

import java.util.List;

public interface ExpenseImportRepository {

    ImportBatchMutationResult createImportBatch(String userId, String fileName, int totalRows);

    ImportBatchResult importExpenseRows(String userId, List<ParsedExpenseImportRow> rows);

    ImportBatchMutationResult updateImportBatch(
            String batchId, int successCount, String status, String errorSummary);
}
