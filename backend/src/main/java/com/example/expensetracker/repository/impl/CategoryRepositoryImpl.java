package com.example.expensetracker.repository.impl;

import com.example.expensetracker.aspect.LoggedOperation;
import com.example.expensetracker.constant.SqlParamNames;
import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.mapper.CategoryRowMapper;
import com.example.expensetracker.repository.model.CategoryResult;
import com.example.expensetracker.repository.model.CategoryRow;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final SimpleJdbcCall getActiveCategoryCall;

    public CategoryRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.getActiveCategoryCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("APP_EXPENSE")
                .withProcedureName("SP_GET_ACTIVE_CATEGORY")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter(SqlParamNames.RESULT_CODE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.RESULT_MESSAGE, Types.VARCHAR),
                        new SqlOutParameter(SqlParamNames.CATEGORY_CURSOR, OracleTypes.CURSOR, new CategoryRowMapper())
                );
    }

    @Override
    @LoggedOperation("app_expense.SP_GET_ACTIVE_CATEGORY")
    public CategoryResult getActiveCategories() {
        Map<String, Object> result = getActiveCategoryCall.execute();

        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);

        @SuppressWarnings("unchecked")
        List<CategoryRow> rows = (List<CategoryRow>) result.get(SqlParamNames.CATEGORY_CURSOR);

        return new CategoryResult(
                rows == null ? List.of() : rows,
                resultCode,
                (String) result.get(SqlParamNames.RESULT_MESSAGE));
    }
}
