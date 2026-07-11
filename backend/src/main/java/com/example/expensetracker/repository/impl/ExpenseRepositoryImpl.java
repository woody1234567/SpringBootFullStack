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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
                .withSchemaName("app_expense")
                .withProcedureName("create_expense");

        this.updateExpenseCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("app_expense")
                .withProcedureName("update_expense");

        this.deleteExpenseCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("app_expense")
                .withProcedureName("delete_expense");

        this.getExpenseDetailCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("app_expense")
                .withProcedureName("get_expense_detail")
                .returningResultSet("expense", new ExpenseRowMapper());

        this.searchExpensesCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("app_expense")
                .withProcedureName("search_expenses")
                .returningResultSet("expenses", new ExpenseRowMapper());
    }

    @Override
    @LoggedOperation("app_expense.create_expense")
    public CreateExpenseResult createExpense(
            Long userId, LocalDate expenseDate, BigDecimal amount, Integer categoryId,
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
        Number expenseId = (Number) result.get(SqlParamNames.EXPENSE_ID);

        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        return new CreateExpenseResult(
                expenseId == null ? null : expenseId.longValue(),
                resultCode,
                (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.update_expense")
    public MutationResult updateExpense(
            Long expenseId, Long userId, LocalDate expenseDate, BigDecimal amount, Integer categoryId,
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
    @LoggedOperation("app_expense.delete_expense")
    public MutationResult deleteExpense(Long expenseId, Long userId) {
        Map<String, Object> params = Map.of(
                SqlParamNames.EXPENSE_ID, expenseId,
                SqlParamNames.USER_ID, userId);

        Map<String, Object> result = deleteExpenseCall.execute(params);
        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        return new MutationResult(resultCode, (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.get_expense_detail")
    public GetExpenseResult getExpenseDetail(Long expenseId, Long userId) {
        Map<String, Object> params = Map.of(
                SqlParamNames.EXPENSE_ID, expenseId,
                SqlParamNames.USER_ID, userId);

        Map<String, Object> result = getExpenseDetailCall.execute(params);
        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        @SuppressWarnings("unchecked")
        List<ExpenseRow> rows = (List<ExpenseRow>) result.get("expense");
        Optional<ExpenseRow> expense = (rows == null || rows.isEmpty()) ? Optional.empty() : Optional.of(rows.get(0));

        return new GetExpenseResult(expense, resultCode, (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }

    @Override
    @LoggedOperation("app_expense.search_expenses")
    public SearchExpensesResult searchExpenses(
            Long userId, LocalDate dateFrom, LocalDate dateTo, Integer categoryId, int page, int pageSize
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
        List<ExpenseRow> rows = (List<ExpenseRow>) result.get("expenses");

        return new SearchExpensesResult(
                rows == null ? List.of() : rows,
                totalCount == null ? 0 : totalCount.intValue(),
                resultCode,
                (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }
}
