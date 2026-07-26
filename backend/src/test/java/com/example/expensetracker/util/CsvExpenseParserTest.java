package com.example.expensetracker.util;

import com.example.expensetracker.exception.ValidationException;
import com.example.expensetracker.repository.model.ParsedExpenseImportBatch;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvExpenseParserTest {

    private final CsvExpenseParser parser = new CsvExpenseParser();

    @Test
    void parsesValidRowsWithJacksonCsv() {
        String csv = """
                expense_date,amount,category,invoice_number,note
                2026-07-23,120.50,餐飲,INV-1,"lunch, with client"
                """;

        ParsedExpenseImportBatch result = parser.parse(file(csv));

        assertThat(result.failedRows()).isEmpty();
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).note()).isEqualTo("lunch, with client");
    }

    @Test
    void returnsFieldErrorMapForInvalidRows() {
        String csv = """
                expense_date,amount,category,invoice_number,note
                2026-02-30,-1,,INV-1,test
                2026-07-24,10,交通,INV-1,test
                """;

        ParsedExpenseImportBatch result = parser.parse(file(csv));

        assertThat(result.failedRows()).hasSize(2);
        assertThat(result.failedRows().get(0).fieldErrors().keySet())
                .contains("expense_date", "amount", "category");
        assertThat(result.failedRows().get(1).fieldErrors())
                .containsEntry("invoice_number", List.of("duplicate invoice_number within file; first seen at row 1"));
    }

    @Test
    void rejectsMissingHeaders() {
        String csv = """
                expense_date,amount,category
                2026-07-23,120.50,餐飲
                """;

        assertThatThrownBy(() -> parser.parse(file(csv)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CSV header must contain");
    }

    private MockMultipartFile file(String csv) {
        return new MockMultipartFile(
                "file",
                "expenses.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
    }
}
