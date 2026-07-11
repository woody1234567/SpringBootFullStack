package com.example.expensetracker.dto.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int totalCount,
        int page,
        int pageSize
) {
}
