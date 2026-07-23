package com.example.expensetracker.repository.impl;

import com.example.expensetracker.aspect.LoggedOperation;
import com.example.expensetracker.constant.SqlParamNames;
import com.example.expensetracker.repository.ExpenseImportRepository;
import com.example.expensetracker.repository.mapper.ImportRowFailureMapper;
import com.example.expensetracker.repository.model.ImportBatchMutationResult;
import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ImportRowFailure;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls APP_EXPENSE import procedures. Spring owns the transaction boundary:
 * batch record calls can run in REQUIRES_NEW, while expense row import runs in
 * the caller's transaction.
 */
@Repository
public class ExpenseImportRepositoryImpl implements ExpenseImportRepository {

    private static final String IMPORT_ROW_OBJECT_TYPE = "APP_EXPENSE.TO_EXPENSE_IMPORT_ROW";
    private static final String IMPORT_ROW_TABLE_TYPE = "APP_EXPENSE.TT_EXPENSE_IMPORT_ROW";

    private final SimpleJdbcCall createImportBatchCall;
    private final SimpleJdbcCall importExpenseRowsCall;
    private final SimpleJdbcCall updateImportBatchCall;

    public ExpenseImportRepositoryImpl(JdbcTemplate jdbcTemplate) {
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

        this.importExpenseRowsCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_IMPORT_EXPENSE_BATCH")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.USER_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.ROWS, Types.ARRAY, IMPORT_ROW_TABLE_TYPE),
                        new SqlOutParameter(SqlParamNames.SUCCESS_COUNT, Types.NUMERIC),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.FAILED_CURSOR, OracleTypes.CURSOR, new ImportRowFailureMapper())
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
    @LoggedOperation("app_expense.SP_IMPORT_EXPENSE_BATCH")
    public ImportBatchResult importExpenseRows(String userId, List<ParsedExpenseImportRow> rows) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.USER_ID, userId);
        params.put(SqlParamNames.ROWS, toOracleRows(rows));

        Map<String, Object> result = importExpenseRowsCall.execute(params);

        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);
        Number successCount = (Number) result.get(SqlParamNames.SUCCESS_COUNT);

        @SuppressWarnings("unchecked")
        List<ImportRowFailure> failedRows = (List<ImportRowFailure>) result.get(SqlParamNames.FAILED_CURSOR);

        return new ImportBatchResult(
                null,
                successCount == null ? 0 : successCount.intValue(),
                resultCode,
                (String) result.get(SqlParamNames.RESULT_MESSAGE),
                failedRows == null ? List.of() : failedRows);
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

    private SqlTypeValue toOracleRows(List<ParsedExpenseImportRow> rows) {
        return new SqlTypeValue() {
            @Override
            public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName)
                    throws SQLException {
                Struct[] structs = new Struct[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    ParsedExpenseImportRow row = rows.get(i);
                    structs[i] = ps.getConnection().createStruct(IMPORT_ROW_OBJECT_TYPE, new Object[]{
                            row.rowNumber(),
                            row.expenseDate() == null ? null : Date.valueOf(row.expenseDate()),
                            row.amount(),
                            row.categoryName(),
                            row.invoiceNumber(),
                            row.note()
                    });
                }

                OracleConnection oracleConnection = ps.getConnection().unwrap(OracleConnection.class);
                ps.setArray(paramIndex, oracleConnection.createOracleArray(IMPORT_ROW_TABLE_TYPE, structs));
            }
        };
    }
}
