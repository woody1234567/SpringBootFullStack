package com.example.expensetracker.repository.model;

import java.util.List;

public record CategoryResult(List<CategoryRow> categories, String resultCode, String resultMessage) {
}
