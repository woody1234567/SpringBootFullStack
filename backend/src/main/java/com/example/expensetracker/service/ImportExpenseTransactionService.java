package com.example.expensetracker.service;

import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.repository.ExpenseImportRepository;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExpenseTransactionService {

    private final ExpenseImportRepository expenseImportRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public ImportBatchResult importExpenseRows(String userId, String batchId, List<ParsedExpenseImportRow> rows) {
        ImportBatchResult result = expenseImportRepository.importExpenseRows(userId, batchId, rows);

        return switch (result.resultCode()) {
            case ResultCode.SUCCESS -> result;
            case ResultCode.VALIDATION_ERROR -> throw new ImportValidationFailureException(
                    result.resultMessage(), result.failedRows());
            default -> {
                log.error("app_expense.SP_INSERT_IMPORTED_EXPENSE returned result_code={}", result.resultCode());
                throw new IllegalStateException("Unable to complete the import operation");
            }
        };
    }
}
