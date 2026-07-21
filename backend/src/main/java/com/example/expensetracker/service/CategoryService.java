package com.example.expensetracker.service;

import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.dto.response.CategoryResponse;
import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.model.CategoryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getActiveCategories() {
        CategoryResult result = categoryRepository.getActiveCategories();
        if (!ResultCode.SUCCESS.equals(result.resultCode())) {
            log.error("app_expense.SP_GET_ACTIVE_CATEGORY returned result_code={}", result.resultCode());
            throw new IllegalStateException("Unable to complete the operation");
        }

        return result.categories().stream()
                .map(row -> new CategoryResponse(row.categoryId(), row.name()))
                .toList();
    }
}
