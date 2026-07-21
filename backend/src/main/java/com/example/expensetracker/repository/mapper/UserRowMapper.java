package com.example.expensetracker.repository.mapper;

import com.example.expensetracker.repository.model.UserRow;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<UserRow> {

    @Override
    public UserRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserRow(
                rs.getString("user_id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                rs.getBoolean("is_active")
        );
    }
}
