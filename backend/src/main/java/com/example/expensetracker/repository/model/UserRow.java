package com.example.expensetracker.repository.model;

public record UserRow(
        Long userId,
        String email,
        String passwordHash,
        String displayName,
        boolean active
) {
}
