package com.example.expensetracker.repository;

import com.example.expensetracker.repository.model.CategoryResult;

public interface CategoryRepository {

    CategoryResult getActiveCategories();
}
