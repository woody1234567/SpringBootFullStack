package com.example.expensetracker.repository.impl;

import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.mapper.CategoryRowMapper;
import com.example.expensetracker.repository.model.CategoryRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private static final String SELECT_ACTIVE_CATEGORIES =
            "SELECT category_id, name FROM app_expense.v_active_categories ORDER BY name";

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CategoryRow> getActiveCategories() {
        return jdbcTemplate.query(SELECT_ACTIVE_CATEGORIES, new CategoryRowMapper());
    }
}
