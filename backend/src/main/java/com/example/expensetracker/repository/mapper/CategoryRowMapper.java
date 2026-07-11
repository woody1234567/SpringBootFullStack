package com.example.expensetracker.repository.mapper;

import com.example.expensetracker.repository.model.CategoryRow;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoryRowMapper implements RowMapper<CategoryRow> {

    @Override
    public CategoryRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryRow(rs.getInt("category_id"), rs.getString("name"));
    }
}
