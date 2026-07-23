package com.example.expensetracker.service;

import com.example.expensetracker.repository.model.ImportRowFailure;

import java.util.List;

class ImportValidationFailureException extends RuntimeException {

    private final List<ImportRowFailure> failedRows;

    ImportValidationFailureException(String message, List<ImportRowFailure> failedRows) {
        super(message);
        this.failedRows = failedRows == null ? List.of() : failedRows;
    }

    List<ImportRowFailure> failedRows() {
        return failedRows;
    }
}
