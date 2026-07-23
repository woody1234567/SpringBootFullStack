package com.example.expensetracker.service;

import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.repository.ExpenseImportRepository;
import com.example.expensetracker.repository.model.ImportBatchMutationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportBatchTransactionService {

    private final ExpenseImportRepository expenseImportRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createImportBatch(String userId, String fileName, int totalRows) {
        ImportBatchMutationResult result = expenseImportRepository.createImportBatch(userId, fileName, totalRows);
        assertSuccess(result, "app_expense.SP_CREATE_IMPORT_BATCH");
        return result.batchId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateImportBatch(String batchId, int successCount, String status, String errorSummary) {
        ImportBatchMutationResult result =
                expenseImportRepository.updateImportBatch(batchId, successCount, status, errorSummary);
        assertSuccess(result, "app_expense.SP_UPDATE_IMPORT_BATCH");
    }

    private void assertSuccess(ImportBatchMutationResult result, String operationName) {
        if (result == null || !ResultCode.SUCCESS.equals(result.resultCode())) {
            String resultCode = result == null ? null : result.resultCode();
            log.error("{} returned result_code={}", operationName, resultCode);
            throw new IllegalStateException("Unable to complete the import operation");
        }
    }
}
