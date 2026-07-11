package com.example.expensetracker.repository.impl;

import com.example.expensetracker.aspect.LoggedOperation;
import com.example.expensetracker.constant.SqlParamNames;
import com.example.expensetracker.repository.UserRepository;
import com.example.expensetracker.repository.mapper.UserRowMapper;
import com.example.expensetracker.repository.model.CreateUserResult;
import com.example.expensetracker.repository.model.FindUserResult;
import com.example.expensetracker.repository.model.UserRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final SimpleJdbcCall createUserCall;
    private final SimpleJdbcCall getUserByEmailCall;

    public UserRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.createUserCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("app_user")
                .withProcedureName("create_user");

        this.getUserByEmailCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("app_user")
                .withProcedureName("get_user_by_email")
                .returningResultSet("users", new UserRowMapper());
    }

    @Override
    @LoggedOperation("app_user.create_user")
    public CreateUserResult createUser(String email, String passwordHash, String displayName) {
        Map<String, Object> params = new HashMap<>();
        params.put(SqlParamNames.EMAIL, email);
        params.put(SqlParamNames.PASSWORD_HASH, passwordHash);
        params.put(SqlParamNames.DISPLAY_NAME, displayName);

        Map<String, Object> result = createUserCall.execute(params);

        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);
        String resultMessage = (String) result.get(SqlParamNames.RESULT_MESSAGE);
        Number userId = (Number) result.get(SqlParamNames.USER_ID);

        return new CreateUserResult(userId == null ? null : userId.longValue(), resultCode, resultMessage);
    }

    @Override
    @LoggedOperation("app_user.get_user_by_email")
    public FindUserResult findByEmail(String email) {
        Map<String, Object> result = getUserByEmailCall.execute(Map.of(SqlParamNames.EMAIL, email));

        String resultCode = (String) result.get(SqlParamNames.RESULT_CODE);
        String resultMessage = (String) result.get(SqlParamNames.RESULT_MESSAGE);

        @SuppressWarnings("unchecked")
        List<UserRow> users = (List<UserRow>) result.get("users");
        Optional<UserRow> user = (users == null || users.isEmpty()) ? Optional.empty() : Optional.of(users.get(0));

        return new FindUserResult(user, resultCode, resultMessage);
    }
}
