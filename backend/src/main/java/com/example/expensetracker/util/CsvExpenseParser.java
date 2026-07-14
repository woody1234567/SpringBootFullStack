package com.example.expensetracker.util;

import com.example.expensetracker.exception.ValidationException;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses an uploaded expense CSV file into typed rows. Only handles safe type
 * coercion and structural validation (header shape, readability) — business
 * validation (unknown category, non-positive amount, duplicate invoice number)
 * is deliberately left to the Oracle stored procedure per the
 * database-centric architecture.
 */
@Component
public class CsvExpenseParser {

    private static final String COL_EXPENSE_DATE = "expense_date";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CATEGORY = "category";
    private static final String COL_INVOICE_NUMBER = "invoice_number";
    private static final String COL_NOTE = "note";
    private static final Set<String> REQUIRED_HEADERS =
            Set.of(COL_EXPENSE_DATE, COL_AMOUNT, COL_CATEGORY, COL_INVOICE_NUMBER, COL_NOTE);
    private static final String UTF8_BOM = "﻿";

    public List<ParsedExpenseImportRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("CSV file is required");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(stripLeadingBom(reader))) {

            validateHeaders(parser);

            List<ParsedExpenseImportRow> rows = new ArrayList<>();
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rows.add(toRow(rowNumber, record));
                rowNumber++;
            }
            return rows;
        } catch (IOException ex) {
            throw new ValidationException("Unable to read the uploaded CSV file");
        }
    }

    private void validateHeaders(CSVParser parser) {
        Set<String> headers = parser.getHeaderNames().stream()
                .map(h -> h.trim().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        if (!headers.containsAll(REQUIRED_HEADERS)) {
            throw new ValidationException(
                    "CSV header must contain: expense_date, amount, category, invoice_number, note");
        }
    }

    private ParsedExpenseImportRow toRow(int rowNumber, CSVRecord record) {
        LocalDate expenseDate = parseDate(get(record, COL_EXPENSE_DATE));
        BigDecimal amount = parseAmount(get(record, COL_AMOUNT));
        String category = blankToNull(get(record, COL_CATEGORY));
        String invoiceNumber = blankToNull(get(record, COL_INVOICE_NUMBER));
        String note = blankToNull(get(record, COL_NOTE));

        return new ParsedExpenseImportRow(rowNumber, expenseDate, amount, category, invoiceNumber, note);
    }

    private String get(CSVRecord record, String column) {
        return record.isMapped(column) ? record.get(column) : null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private java.io.Reader stripLeadingBom(InputStreamReader reader) throws IOException {
        var buffered = new java.io.BufferedReader(reader);
        buffered.mark(1);
        int first = buffered.read();
        if (first != UTF8_BOM.charAt(0)) {
            buffered.reset();
        }
        return buffered;
    }
}
