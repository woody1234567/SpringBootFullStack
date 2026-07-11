package com.example.expensetracker.repository.model;

public record ImportRowFailure(int rowNumber, String errorMessage) {
}
