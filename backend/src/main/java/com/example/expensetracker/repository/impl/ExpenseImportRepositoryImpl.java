package com.example.expensetracker.repository.impl;

import com.example.expensetracker.aspect.LoggedOperation;
import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.constant.SqlParamNames;
import com.example.expensetracker.repository.ExpenseImportRepository;
import com.example.expensetracker.repository.model.ImportBatchMutationResult;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ImportRowFailure;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ExpenseImportRepositoryImpl implements ExpenseImportRepository {

    private static final String INSERT_IMPORTED_EXPENSE_SQL =
            "CALL app_expense.SP_INSERT_IMPORTED_EXPENSE(?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String CREATE_IMPORT_FAILED_ROW_SQL =
            "CALL app_expense.SP_CREATE_IMPORT_FAILED_ROW(?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall createImportBatchCall;
    private final SimpleJdbcCall updateImportBatchCall;

    public ExpenseImportRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.createImportBatchCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_CREATE_IMPORT_BATCH")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.USER_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.FILE_NAME, Types.NVARCHAR),
                        new SqlParameter(SqlParamNames.TOTAL_ROWS, Types.NUMERIC),
                        new SqlOutParameter(SqlParamNames.BATCH_ID, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR)
                );

        this.updateImportBatchCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_UPDATE_IMPORT_BATCH")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.BATCH_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.SUCCESS_COUNT, Types.NUMERIC),
                        new SqlParameter(SqlParamNames.STATUS, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.ERROR_SUMMARY, Types.NVARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR)
                );
    }

    @Override
    @LoggedOperation("app_expense.SP_CREATE_IMPORT_BATCH")
    public ImportBatchMutationResult createImportBatch(String userId, String fileName, int totalRows) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.USER_ID, userId);
        params.put(SqlParamNames.FILE_NAME, fileName);
        params.put(SqlParamNames.TOTAL_ROWS, totalRows);

        Map<String, Object> result = createImportBatchCall.execute(params);

        return new ImportBatchMutationResult(
                (String) result.get(SqlParamNames.BATCH_ID),
                (String) result.get(SqlParamNames.RESULT_CODE),
                (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.SP_INSERT_IMPORTED_EXPENSE")
    public ImportBatchResult importExpenseRows(String userId, String batchId, List<ParsedExpenseImportRow> rows) {
        int[] updates = jdbcTemplate.batchUpdate(INSERT_IMPORTED_EXPENSE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ParsedExpenseImportRow row = rows.get(i);
                ps.setString(1, userId);
                ps.setString(2, batchId);
                ps.setInt(3, row.rowNumber());
                ps.setDate(4, Date.valueOf(row.expenseDate()));
                ps.setBigDecimal(5, row.amount());
                ps.setString(6, row.categoryName());
                ps.setString(7, row.invoiceNumber());
                ps.setString(8, row.note());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        return new ImportBatchResult(null, updates.length, ResultCode.SUCCESS, "ok", List.of());
    }

    @Override
    @LoggedOperation("app_expense.SP_CREATE_IMPORT_FAILED_ROW")
    public void createImportFailedRows(String batchId, List<ImportRowFailure> failedRows) {
        if (failedRows == null || failedRows.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(CREATE_IMPORT_FAILED_ROW_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ImportRowFailure row = failedRows.get(i);
                ps.setString(1, batchId);
                ps.setInt(2, row.rowNumber());
                ps.setString(3, row.errorMessage());
            }

            @Override
            public int getBatchSize() {
                return failedRows.size();
            }
        });
    }

    @Override
    @LoggedOperation("app_expense.SP_UPDATE_IMPORT_BATCH")
    public ImportBatchMutationResult updateImportBatch(
            String batchId, int successCount, String status, String errorSummary
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.BATCH_ID, batchId);
        params.put(SqlParamNames.SUCCESS_COUNT, successCount);
        params.put(SqlParamNames.STATUS, status);
        params.put(SqlParamNames.ERROR_SUMMARY, errorSummary);

        Map<String, Object> result = updateImportBatchCall.execute(params);

        return new ImportBatchMutationResult(
                batchId,
                (String) result.get(SqlParamNames.RESULT_CODE),
                (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }
}
