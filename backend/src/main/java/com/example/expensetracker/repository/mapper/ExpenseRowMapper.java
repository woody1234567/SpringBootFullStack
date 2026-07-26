package com.example.expensetracker.repository.mapper;

import com.example.expensetracker.repository.model.ExpenseRow;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExpenseRowMapper implements RowMapper<ExpenseRow> {

    @Override
    public ExpenseRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ExpenseRow(
                rs.getString("expense_id"),
                rs.getDate("expense_date").toLocalDate(),
                rs.getBigDecimal("amount"),
                rs.getString("category_id"),
                rs.getString("category_name"),
                rs.getString("invoice_number"),
                rs.getString("note"),
                rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("update_time").toLocalDateTime()
        );
    }
}
