package com.example.expensetracker.repository.model;

public record UserRow(
        String userId,
        String email,
        String passwordHash,
        String displayName,
        boolean active
) {
}
