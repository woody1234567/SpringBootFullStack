package com.example.expensetracker.repository.model;

/** Generic outcome for stored procedures that only report a result code/message (update, delete). */
public record MutationResult(String resultCode, String resultMessage) {
}
