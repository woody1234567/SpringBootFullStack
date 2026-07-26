package com.example.expensetracker.service;

import com.example.expensetracker.dto.response.ImportResultResponse;
import com.example.expensetracker.dto.response.ImportRowError;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ImportRowFailure;
import com.example.expensetracker.repository.model.ParsedExpenseImportBatch;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import com.example.expensetracker.util.CsvExpenseParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ExpenseImportService {

    private static final Pattern SAFE_ROW_ERROR = Pattern.compile("(Row\\s+(\\d+)\\s*:\\s*[^\\r\\n]+)");

    private final CsvExpenseParser csvExpenseParser;
    private final ImportExpenseTransactionService importExpenseTransactionService;
    private final ImportBatchTransactionService importBatchTransactionService;

    public ImportResultResponse importExpenses(String userId, MultipartFile file) {
        ParsedExpenseImportBatch parsedBatch = csvExpenseParser.parse(file);
        List<ParsedExpenseImportRow> rows = parsedBatch.rows();
        String batchId = importBatchTransactionService.createImportBatch(
                userId, file.getOriginalFilename(), rows.size());

        if (parsedBatch.hasFailures()) {
            importBatchTransactionService.createImportFailedRows(batchId, parsedBatch.failedRows());
            String errorSummary = firstFailureMessage(parsedBatch.failedRows(), "CSV row validation failed");
            importBatchTransactionService.updateImportBatch(batchId, 0, "FAILED", errorSummary);
            return toResponse(batchId, rows.size(), 0, "FAILED", parsedBatch.failedRows());
        }

        try {
            ImportBatchResult result = importExpenseTransactionService.importExpenseRows(userId, batchId, rows);
            importBatchTransactionService.updateImportBatch(batchId, result.successCount(), "SUCCESS", null);
            return toResponse(batchId, rows.size(), result.successCount(), "SUCCESS", List.of());
        } catch (ImportValidationFailureException ex) {
            importBatchTransactionService.createImportFailedRows(batchId, ex.failedRows());
            String errorSummary = firstFailureMessage(ex.failedRows(), ex.getMessage());
            importBatchTransactionService.updateImportBatch(batchId, 0, "FAILED", errorSummary);
            return toResponse(batchId, rows.size(), 0, "FAILED", ex.failedRows());
        } catch (RuntimeException ex) {
            List<ImportRowFailure> failedRows = List.of(toDatabaseFailure(ex));
            importBatchTransactionService.createImportFailedRows(batchId, failedRows);
            importBatchTransactionService.updateImportBatch(
                    batchId, 0, "FAILED", firstFailureMessage(failedRows, "System error during import"));
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
                .map(f -> new ImportRowError(
                        f.rowNumber(),
                        f.errorMessage(),
                        f.fieldErrors() == null ? Map.of() : f.fieldErrors()))
                .toList();

        return new ImportResultResponse(batchId, totalRows, successCount, status, rowErrors);
    }

    private ImportRowFailure toDatabaseFailure(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            Matcher matcher = message == null ? null : SAFE_ROW_ERROR.matcher(message);
            if (matcher != null && matcher.find()) {
                return new ImportRowFailure(Integer.parseInt(matcher.group(2)), matcher.group(1));
            }
            current = current.getCause();
        }
        return new ImportRowFailure(0, "Database import failed; no rows were imported");
    }
}
