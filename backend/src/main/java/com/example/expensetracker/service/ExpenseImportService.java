package com.example.expensetracker.service;

import com.example.expensetracker.dto.response.ImportResultResponse;
import com.example.expensetracker.dto.response.ImportRowError;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ImportRowFailure;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import com.example.expensetracker.util.CsvExpenseParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseImportService {

    private final CsvExpenseParser csvExpenseParser;
    private final ImportExpenseTransactionService importExpenseTransactionService;
    private final ImportBatchTransactionService importBatchTransactionService;

    public ImportResultResponse importExpenses(String userId, MultipartFile file) {
        List<ParsedExpenseImportRow> rows = csvExpenseParser.parse(file);
        String batchId = importBatchTransactionService.createImportBatch(
                userId, file.getOriginalFilename(), rows.size());

        try {
            ImportBatchResult result = importExpenseTransactionService.importExpenseRows(userId, rows);
            importBatchTransactionService.updateImportBatch(batchId, result.successCount(), "SUCCESS", null);
            return toResponse(batchId, rows.size(), result.successCount(), "SUCCESS", List.of());
        } catch (ImportValidationFailureException ex) {
            String errorSummary = firstFailureMessage(ex.failedRows(), ex.getMessage());
            importBatchTransactionService.updateImportBatch(batchId, 0, "FAILED", errorSummary);
            return toResponse(batchId, rows.size(), 0, "FAILED", ex.failedRows());
        } catch (RuntimeException ex) {
            importBatchTransactionService.updateImportBatch(batchId, 0, "FAILED", "System error during import");
            throw ex;
        }
    }

    String firstFailureMessage(List<ImportRowFailure> failedRows, String fallback) {
        if (failedRows == null || failedRows.isEmpty()) {
            return fallback;
        }
        return failedRows.get(0).errorMessage();
    }

    private ImportResultResponse toResponse(
            String batchId, int totalRows, int successCount, String status, List<ImportRowFailure> failedRows
    ) {
        List<ImportRowError> rowErrors = failedRows.stream()
                .map(f -> new ImportRowError(f.rowNumber(), f.errorMessage()))
                .toList();

        return new ImportResultResponse(batchId, totalRows, successCount, status, rowErrors);
    }
}
