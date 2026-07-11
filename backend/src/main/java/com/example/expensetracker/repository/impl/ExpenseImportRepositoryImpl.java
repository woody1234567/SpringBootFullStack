package com.example.expensetracker.repository.impl;

import com.example.expensetracker.aspect.LoggedOperation;
import com.example.expensetracker.constant.SqlParamNames;
import com.example.expensetracker.repository.ExpenseImportRepository;
import com.example.expensetracker.repository.mapper.ImportRowFailureMapper;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ImportRowFailure;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import microsoft.sql.Types;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls app_expense.import_expenses_batch, passing the CSV rows as a single
 * Table-Valued Parameter so SQL Server performs the whole-batch validation and
 * atomic insert in one round trip (see .claude/CLAUDE.md section 9 — database-
 * owned transaction pattern).
 */
@Repository
public class ExpenseImportRepositoryImpl implements ExpenseImportRepository {

    private static final String TVP_TYPE_NAME = "app_expense.expense_import_row_type";

    private final SimpleJdbcCall importBatchCall;

    public ExpenseImportRepositoryImpl(JdbcTemplate jdbcTemplate) {
        // Metadata lookup for TVP-accepting procedures is unreliable, so parameters
        // are declared explicitly per .claude/CLAUDE.md section 6.
        this.importBatchCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("app_expense")
                .withProcedureName("import_expenses_batch")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.USER_ID, java.sql.Types.BIGINT),
                        new SqlParameter(SqlParamNames.FILE_NAME, java.sql.Types.NVARCHAR),
                        new SqlParameter(SqlParamNames.ROWS, Types.STRUCTURED, TVP_TYPE_NAME),
                        new SqlOutParameter(SqlParamNames.BATCH_ID, java.sql.Types.BIGINT),
                        new SqlOutParameter(SqlParamNames.SUCCESS_COUNT, java.sql.Types.INTEGER),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, java.sql.Types.NVARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, java.sql.Types.NVARCHAR)
                )
                .returningResultSet("failed_rows", new ImportRowFailureMapper());
    }

    @Override
    @LoggedOperation("app_expense.import_expenses_batch")
    public ImportBatchResult importBatch(Long userId, String fileName, List<ParsedExpenseImportRow> rows) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.USER_ID, userId);
        params.put(SqlParamNames.FILE_NAME, fileName);
        params.put(SqlParamNames.ROWS, toDataTable(rows));

        Map<String, Object> result = importBatchCall.execute(params);

        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);
        Number batchId = (Number) result.get(SqlParamNames.BATCH_ID);
        Number successCount = (Number) result.get(SqlParamNames.SUCCESS_COUNT);

        @SuppressWarnings("unchecked")
        List<ImportRowFailure> failedRows = (List<ImportRowFailure>) result.get("failed_rows");

        return new ImportBatchResult(
                batchId == null ? null : batchId.longValue(),
                successCount == null ? 0 : successCount.intValue(),
                resultCode,
                (String) result.get(SqlParamNames.RESULT_MESSAGE),
                failedRows == null ? List.of() : failedRows);
    }

    private SQLServerDataTable toDataTable(List<ParsedExpenseImportRow> rows) {
        try {
            SQLServerDataTable table = new SQLServerDataTable();
            table.addColumnMetadata("row_number", java.sql.Types.INTEGER);
            table.addColumnMetadata("expense_date", java.sql.Types.DATE);
            table.addColumnMetadata("amount", java.sql.Types.DECIMAL);
            table.addColumnMetadata("category_name", java.sql.Types.NVARCHAR);
            table.addColumnMetadata("invoice_number", java.sql.Types.NVARCHAR);
            table.addColumnMetadata("note", java.sql.Types.NVARCHAR);

            for (ParsedExpenseImportRow row : rows) {
                table.addRow(
                        row.rowNumber(),
                        row.expenseDate() == null ? null : Date.valueOf(row.expenseDate()),
                        row.amount(),
                        row.categoryName(),
                        row.invoiceNumber(),
                        row.note());
            }
            return table;
        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException("Unable to build the CSV import table parameter", ex);
        }
    }
}
