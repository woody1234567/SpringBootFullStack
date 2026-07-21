package com.example.expensetracker.dto.response;

public record UserResponse(
        String userId,
        String email,
        String displayName
) {
}
