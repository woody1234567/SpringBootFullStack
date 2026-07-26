package com.example.expensetracker.repository.model;

import java.util.List;
import java.util.Map;

public record ImportRowFailure(
        int rowNumber,
        String errorMessage,
        Map<String, List<String>> fieldErrors
) {
    public ImportRowFailure(int rowNumber, String errorMessage) {
        this(rowNumber, errorMessage, Map.of());
    }
}
