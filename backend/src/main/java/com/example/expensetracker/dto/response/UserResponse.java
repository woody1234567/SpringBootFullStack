package com.example.expensetracker.dto.response;

public record UserResponse(
        Long userId,
        String email,
        String displayName
) {
}
