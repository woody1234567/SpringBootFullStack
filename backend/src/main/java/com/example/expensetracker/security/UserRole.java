package com.example.expensetracker.security;

import java.util.Locale;
import java.util.Optional;

public enum UserRole {
    ADMIN("admin", "ROLE_ADMIN"),
    USER("user", "ROLE_USER");

    private final String value;
    private final String authority;

    UserRole(String value, String authority) {
        this.value = value;
        this.authority = authority;
    }

    public String value() {
        return value;
    }

    public String authority() {
        return authority;
    }

    public static Optional<UserRole> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        for (UserRole role : values()) {
            if (role.value.equals(normalizedValue)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    public static UserRole require(String value) {
        return fromValue(value)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported user role"));
    }
}
