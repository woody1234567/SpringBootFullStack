package com.example.expensetracker.service;

import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.dto.response.ImportResultResponse;
import com.example.expensetracker.repository.ExpenseImportRepository;
import com.example.expensetracker.repository.model.ImportBatchMutationResult;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ImportRowFailure;
import com.example.expensetracker.repository.model.ParsedExpenseImportBatch;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import com.example.expensetracker.util.CsvExpenseParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseImportServiceTest {

    private static final String USER_ID = "U1";
    private static final String FILE_NAME = "expenses.csv";
    private static final String BATCH_ID = "B1";

    private final MultipartFile file = new MockMultipartFile("file", FILE_NAME, "text/csv", new byte[0]);

    @Test
    void importsRowsAndFinalizesBatchInSeparateTransactions() {
        List<ParsedExpenseImportRow> rows = rows();
        FakeExpenseImportRepository repository = new FakeExpenseImportRepository();
        repository.importResult = new ImportBatchResult(null, 1, ResultCode.SUCCESS, "ok", List.of());
        ExpenseImportService service = service(rows, repository);

        ImportResultResponse response = service.importExpenses(USER_ID, file);

        assertThat(response.batchId()).isEqualTo(BATCH_ID);
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.totalRows()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedRows()).isEmpty();
        assertThat(repository.calls()).containsExactly(
                "create:U1:expenses.csv:1",
                "import:U1:B1:1",
                "update:B1:1:SUCCESS:null");
    }

    @Test
    void recordsJavaFieldValidationFailuresWithoutImportingExpenses() {
        List<ParsedExpenseImportRow> rows = rows();
        List<ImportRowFailure> failedRows = List.of(new ImportRowFailure(
                1,
                "amount: amount must be greater than 0",
                Map.of("amount", List.of("amount must be greater than 0"))));
        FakeExpenseImportRepository repository = new FakeExpenseImportRepository();
        ExpenseImportService service = service(new ParsedExpenseImportBatch(rows, failedRows), repository);

        ImportResultResponse response = service.importExpenses(USER_ID, file);

        assertThat(response.batchId()).isEqualTo(BATCH_ID);
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.successCount()).isZero();
        assertThat(response.failedRows()).hasSize(1);
        assertThat(response.failedRows().get(0).fieldErrors())
                .containsEntry("amount", List.of("amount must be greater than 0"));
        assertThat(repository.calls()).containsExactly(
                "create:U1:expenses.csv:1",
                "failed:B1:1",
                "update:B1:0:FAILED:amount: amount must be greater than 0");
    }

    @Test
    void rollsBackExpenseTransactionButKeepsFailedBatchRecordForValidationFailure() {
        List<ParsedExpenseImportRow> rows = rows();
        List<ImportRowFailure> failedRows = List.of(new ImportRowFailure(1, "Row 1: category not found"));
        FakeExpenseImportRepository repository = new FakeExpenseImportRepository();
        repository.importResult = new ImportBatchResult(null, 0, ResultCode.VALIDATION_ERROR, "invalid", failedRows);
        ExpenseImportService service = service(new ParsedExpenseImportBatch(rows, List.of()), repository);

        ImportResultResponse response = service.importExpenses(USER_ID, file);

        assertThat(response.batchId()).isEqualTo(BATCH_ID);
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.successCount()).isZero();
        assertThat(response.failedRows()).hasSize(1);
        assertThat(response.failedRows().get(0).message()).isEqualTo("Row 1: category not found");
        assertThat(repository.calls()).containsExactly(
                "create:U1:expenses.csv:1",
                "import:U1:B1:1",
                "failed:B1:1",
                "update:B1:0:FAILED:Row 1: category not found");
    }

    @Test
    void rollsBackExpenseTransactionAndMarksBatchFailedWhenImportThrows() {
        List<ParsedExpenseImportRow> rows = rows();
        RuntimeException importFailure = new IllegalStateException("database unavailable");
        FakeExpenseImportRepository repository = new FakeExpenseImportRepository();
        repository.importFailure = importFailure;
        ExpenseImportService service = service(new ParsedExpenseImportBatch(rows, List.of()), repository);

        assertThatThrownBy(() -> service.importExpenses(USER_ID, file)).isSameAs(importFailure);

        assertThat(repository.calls()).containsExactly(
                "create:U1:expenses.csv:1",
                "import:U1:B1:1",
                "failed:B1:1",
                "update:B1:0:FAILED:Database import failed; no rows were imported");
    }

    @Test
    void importExpenseRowsUsesRequiredTransaction() throws NoSuchMethodException {
        Method method = ImportExpenseTransactionService.class
                .getMethod("importExpenseRows", String.class, String.class, List.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRED);
    }

    @Test
    void importBatchMutationsUseRequiresNewTransactions() throws NoSuchMethodException {
        Method createMethod = ImportBatchTransactionService.class
                .getMethod("createImportBatch", String.class, String.class, int.class);
        Method updateMethod = ImportBatchTransactionService.class
                .getMethod("updateImportBatch", String.class, int.class, String.class, String.class);
        Method failedRowsMethod = ImportBatchTransactionService.class
                .getMethod("createImportFailedRows", String.class, List.class);

        assertThat(createMethod.getAnnotation(Transactional.class).propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
        assertThat(updateMethod.getAnnotation(Transactional.class).propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
        assertThat(failedRowsMethod.getAnnotation(Transactional.class).propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }

    private ExpenseImportService service(
            List<ParsedExpenseImportRow> rows, FakeExpenseImportRepository repository
    ) {
        return service(new ParsedExpenseImportBatch(rows, List.of()), repository);
    }

    private ExpenseImportService service(
            ParsedExpenseImportBatch parsedBatch, FakeExpenseImportRepository repository
    ) {
        return new ExpenseImportService(
                new FakeCsvExpenseParser(parsedBatch),
                new ImportExpenseTransactionService(repository),
                new ImportBatchTransactionService(repository));
    }

    private List<ParsedExpenseImportRow> rows() {
        return List.of(new ParsedExpenseImportRow(
                1, LocalDate.of(2026, 7, 23), BigDecimal.TEN, "Meals", "INV-1", "Lunch"));
    }

    private static final class FakeCsvExpenseParser extends CsvExpenseParser {

        private final ParsedExpenseImportBatch parsedBatch;

        private FakeCsvExpenseParser(ParsedExpenseImportBatch parsedBatch) {
            this.parsedBatch = parsedBatch;
        }

        @Override
        public ParsedExpenseImportBatch parse(MultipartFile file) {
            return parsedBatch;
        }
    }

    private static final class FakeExpenseImportRepository implements ExpenseImportRepository {

        private final List<String> calls = new ArrayList<>();
        private ImportBatchResult importResult;
        private RuntimeException importFailure;

        @Override
        public ImportBatchMutationResult createImportBatch(String userId, String fileName, int totalRows) {
            calls.add("create:%s:%s:%d".formatted(userId, fileName, totalRows));
            return successBatchMutation();
        }

        @Override
        public ImportBatchResult importExpenseRows(String userId, String batchId, List<ParsedExpenseImportRow> rows) {
            calls.add("import:%s:%s:%d".formatted(userId, batchId, rows.size()));
            if (importFailure != null) {
                throw importFailure;
            }
            return importResult;
        }

        @Override
        public void createImportFailedRows(String batchId, List<ImportRowFailure> failedRows) {
            calls.add("failed:%s:%d".formatted(batchId, failedRows.size()));
        }

        @Override
        public ImportBatchMutationResult updateImportBatch(
                String batchId, int successCount, String status, String errorSummary
        ) {
            calls.add("update:%s:%d:%s:%s".formatted(batchId, successCount, status, errorSummary));
            return successBatchMutation();
        }

        List<String> calls() {
            return calls;
        }

        private ImportBatchMutationResult successBatchMutation() {
            return new ImportBatchMutationResult(BATCH_ID, ResultCode.SUCCESS, "ok");
        }
    }
}
