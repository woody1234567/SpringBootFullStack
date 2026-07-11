package com.example.expensetracker.dto.response;

import java.util.List;

/**
 * Standard API response envelope used by every controller, per
 * .claude/CLAUDE.md section 15.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode,
        List<String> errors
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, List<String> errors) {
        return new ApiResponse<>(false, message, null, errorCode, errors);
    }
}
