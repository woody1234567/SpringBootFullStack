package com.example.expensetracker.service;

import com.example.expensetracker.dto.response.CategoryResponse;
import com.example.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.getActiveCategories().stream()
                .map(row -> new CategoryResponse(row.categoryId(), row.name()))
                .toList();
    }
}
