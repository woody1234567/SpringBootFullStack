package com.example.expensetracker.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
