package com.example.expensetracker.repository.model;

import java.util.Optional;

public record FindUserResult(Optional<UserRow> user, String resultCode, String resultMessage) {
}
