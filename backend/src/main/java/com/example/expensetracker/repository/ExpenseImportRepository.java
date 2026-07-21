package com.example.expensetracker.repository;

import com.example.expensetracker.repository.model.ImportBatchResult;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;

import java.util.List;

public interface ExpenseImportRepository {

    ImportBatchResult importBatch(String userId, String fileName, List<ParsedExpenseImportRow> rows);
}
