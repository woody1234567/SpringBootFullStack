package com.example.expensetracker.dto.response;

import java.util.List;
import java.util.Map;

public record ImportRowError(
        int rowNumber,
        String message,
        Map<String, List<String>> fieldErrors
) {
}
