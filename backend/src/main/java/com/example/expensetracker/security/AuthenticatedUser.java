package com.example.expensetracker.security;

/** Principal populated into the Spring Security context from a validated JWT. */
public record AuthenticatedUser(String userId, String email) {
}
