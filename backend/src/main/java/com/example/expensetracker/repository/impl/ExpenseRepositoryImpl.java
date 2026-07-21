package com.example.expensetracker.repository.impl;

import com.example.expensetracker.aspect.LoggedOperation;
import com.example.expensetracker.constant.SqlParamNames;
import com.example.expensetracker.repository.ExpenseRepository;
import com.example.expensetracker.repository.mapper.ExpenseRowMapper;
import com.example.expensetracker.repository.model.CreateExpenseResult;
import com.example.expensetracker.repository.model.ExpenseRow;
import com.example.expensetracker.repository.model.GetExpenseResult;
import com.example.expensetracker.repository.model.MutationResult;
import com.example.expensetracker.repository.model.SearchExpensesResult;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final SimpleJdbcCall createExpenseCall;
    private final SimpleJdbcCall updateExpenseCall;
    private final SimpleJdbcCall deleteExpenseCall;
    private final SimpleJdbcCall getExpenseDetailCall;
    private final SimpleJdbcCall searchExpensesCall;

    public ExpenseRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.createExpenseCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_CREATE_EXPENSE")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.USER_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.EXPENSE_DATE, Types.DATE),
                        new SqlParameter(SqlParamNames.AMOUNT, Types.NUMERIC),
                        new SqlParameter(SqlParamNames.CATEGORY_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.INVOICE_NUMBER, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.NOTE, Types.NVARCHAR),
                        new SqlOutParameter(SqlParamNames.EXPENSE_ID, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR)
                );

        this.updateExpenseCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_UPDATE_EXPENSE")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.EXPENSE_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.USER_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.EXPENSE_DATE, Types.DATE),
                        new SqlParameter(SqlParamNames.AMOUNT, Types.NUMERIC),
                        new SqlParameter(SqlParamNames.CATEGORY_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.INVOICE_NUMBER, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.NOTE, Types.NVARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR)
                );

        this.deleteExpenseCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_DELETE_EXPENSE")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.EXPENSE_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.USER_ID, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR)
                );

        this.getExpenseDetailCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_GET_EXPENSE_DETAIL")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.EXPENSE_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.USER_ID, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.EXPENSE_CURSOR, OracleTypes.CURSOR, new ExpenseRowMapper())
                );

        this.searchExpensesCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_SEARCH_EXPENSE")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(SqlParamNames.USER_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.DATE_FROM, Types.DATE),
                        new SqlParameter(SqlParamNames.DATE_TO, Types.DATE),
                        new SqlParameter(SqlParamNames.CATEGORY_ID, Types.VARCHAR),
                        new SqlParameter(SqlParamNames.PAGE, Types.NUMERIC),
                        new SqlParameter(SqlParamNames.PAGE_SIZE, Types.NUMERIC),
                        new SqlOutParameter(SqlParamNames.TOTAL_COUNT, Types.NUMERIC),
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.EXPENSE_CURSOR, OracleTypes.CURSOR, new ExpenseRowMapper())
                );
    }

    @Override
    @LoggedOperation("app_expense.SP_CREATE_EXPENSE")
    public CreateExpenseResult createExpense(
            String userId, LocalDate expenseDate, BigDecimal amount, String categoryId,
            String invoiceNumber, String note
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.USER_ID, userId);
        params.put(SqlParamNames.EXPENSE_DATE, expenseDate);
        params.put(SqlParamNames.AMOUNT, amount);
        params.put(SqlParamNames.CATEGORY_ID, categoryId);
        params.put(SqlParamNames.INVOICE_NUMBER, invoiceNumber);
        params.put(SqlParamNames.NOTE, note);

        Map<String, Object> result = createExpenseCall.execute(params);
        String expenseId = (String) result.get(SqlParamNames.EXPENSE_ID);

        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        return new CreateExpenseResult(
                expenseId,
                resultCode,
                (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.SP_UPDATE_EXPENSE")
    public MutationResult updateExpense(
            String expenseId, String userId, LocalDate expenseDate, BigDecimal amount, String categoryId,
            String invoiceNumber, String note
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.EXPENSE_ID, expenseId);
        params.put(SqlParamNames.USER_ID, userId);
        params.put(SqlParamNames.EXPENSE_DATE, expenseDate);
        params.put(SqlParamNames.AMOUNT, amount);
        params.put(SqlParamNames.CATEGORY_ID, categoryId);
        params.put(SqlParamNames.INVOICE_NUMBER, invoiceNumber);
        params.put(SqlParamNames.NOTE, note);

        Map<String, Object> result = updateExpenseCall.execute(params);
        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        return new MutationResult(resultCode, (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.SP_DELETE_EXPENSE")
    public MutationResult deleteExpense(String expenseId, String userId) {
        Map<String, Object> params = Map.of(
                SqlParamNames.EXPENSE_ID, expenseId,
                SqlParamNames.USER_ID, userId);

        Map<String, Object> result = deleteExpenseCall.execute(params);
        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        return new MutationResult(resultCode, (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.SP_GET_EXPENSE_DETAIL")
    public GetExpenseResult getExpenseDetail(String expenseId, String userId) {
        Map<String, Object> params = Map.of(
                SqlParamNames.EXPENSE_ID, expenseId,
                SqlParamNames.USER_ID, userId);

        Map<String, Object> result = getExpenseDetailCall.execute(params);
        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        @SuppressWarnings("unchecked")
        List<ExpenseRow> rows = (List<ExpenseRow>) result.get(SqlParamNames.EXPENSE_CURSOR);
        Optional<ExpenseRow> expense = (rows == null || rows.isEmpty()) ? Optional.empty() : Optional.of(rows.get(0));

        return new GetExpenseResult(expense, resultCode, (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.SP_SEARCH_EXPENSE")
    public SearchExpensesResult searchExpenses(
            String userId, LocalDate dateFrom, LocalDate dateTo, String categoryId, int page, int pageSize
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.USER_ID, userId);
        params.put(SqlParamNames.DATE_FROM, dateFrom);
        params.put(SqlParamNames.DATE_TO, dateTo);
        params.put(SqlParamNames.CATEGORY_ID, categoryId);
        params.put(SqlParamNames.PAGE, page);
        params.put(SqlParamNames.PAGE_SIZE, pageSize);

        Map<String, Object> result = searchExpensesCall.execute(params);
        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);
        Number totalCount = (Number) result.get(SqlParamNames.TOTAL_COUNT);

        @SuppressWarnings("unchecked")
        List<ExpenseRow> rows = (List<ExpenseRow>) result.get(SqlParamNames.EXPENSE_CURSOR);

        return new SearchExpensesResult(
                rows == null ? List.of() : rows,
                totalCount == null ? 0 : totalCount.intValue(),
                resultCode,
                (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }
}
