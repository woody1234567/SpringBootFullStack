package com.example.expensetracker.repository;

import com.example.expensetracker.repository.model.CategoryRow;

import java.util.List;

public interface CategoryRepository {

    List<CategoryRow> getActiveCategories();
}
