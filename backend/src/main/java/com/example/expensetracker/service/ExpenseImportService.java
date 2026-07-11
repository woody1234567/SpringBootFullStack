package com.example.expensetracker.service;

import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.dto.response.ImportResultResponse;
import com.example.expensetracker.dto.response.ImportRowError;
import com.example.expensetracker.repository.ExpenseImportRepository;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import com.example.expensetracker.util.CsvExpenseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseImportService {

    private final CsvExpenseParser csvExpenseParser;
    private final ExpenseImportRepository expenseImportRepository;

    /**
     * The whole-batch atomicity guarantee is already owned by SQL Server —
     * app_expense.import_expenses_batch validates every row and either inserts
     * all of them or none, in a single self-contained transaction. {@code
     * @Transactional} here wraps exactly one repository call (not several) and
     * is kept per explicit product requirement / as a connection-consistency
     * safety net, not because Java needs to coordinate multiple writes.
     */
    @Transactional
    public ImportResultResponse importExpenses(Long userId, MultipartFile file) {
        List<ParsedExpenseImportRow> rows = csvExpenseParser.parse(file);

        log.info("Importing {} CSV rows for user_id={}", rows.size(), userId);
        ImportBatchResult result = expenseImportRepository.importBatch(userId, file.getOriginalFilename(), rows);

        String status = switch (result.resultCode()) {
            case ResultCode.SUCCESS -> "SUCCESS";
            case ResultCode.VALIDATION_ERROR -> "FAILED";
            default -> {
                log.error("app_expense.import_expenses_batch returned result_code={}", result.resultCode());
                throw new IllegalStateException("Unable to complete the import operation");
            }
        };

        List<ImportRowError> failedRows = result.failedRows().stream()
                .map(f -> new ImportRowError(f.rowNumber(), f.errorMessage()))
                .toList();

        return new ImportResultResponse(result.batchId(), rows.size(), result.successCount(), status, failedRows);
    }
}
