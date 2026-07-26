package com.example.expensetracker.util;

import com.example.expensetracker.exception.ValidationException;
import com.example.expensetracker.repository.model.ImportRowFailure;
import com.example.expensetracker.repository.model.ParsedExpenseImportBatch;
import com.example.expensetracker.repository.model.ParsedExpenseImportRow;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class CsvExpenseParser {

    private static final String COL_EXPENSE_DATE = "expense_date";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CATEGORY = "category";
    private static final String COL_INVOICE_NUMBER = "invoice_number";
    private static final String COL_NOTE = "note";
    private static final Set<String> REQUIRED_HEADERS =
            Set.of(COL_EXPENSE_DATE, COL_AMOUNT, COL_CATEGORY, COL_INVOICE_NUMBER, COL_NOTE);

    private final CsvMapper csvMapper = new CsvMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public ParsedExpenseImportBatch parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("CSV file is required");
        }

        String content = readContent(file);
        validateHeaders(content);

        CsvSchema schema = CsvSchema.emptySchema()
                .withHeader()
                .withColumnReordering(true)
                .withNullValue("");

        try {
            MappingIterator<CsvExpenseUploadRow> iterator = csvMapper
                    .readerFor(CsvExpenseUploadRow.class)
                    .with(schema)
                    .readValues(content);

            List<ParsedExpenseImportRow> rows = new ArrayList<>();
            List<ImportRowFailure> failedRows = new ArrayList<>();
            Map<String, Integer> invoiceRowNumbers = new HashMap<>();
            int rowNumber = 1;

            while (iterator.hasNext()) {
                CsvExpenseUploadRow csvRow = iterator.next();
                Map<String, List<String>> fieldErrors = validateRow(csvRow);
                addDuplicateInvoiceError(csvRow.invoiceNumber(), rowNumber, invoiceRowNumbers, fieldErrors);

                ParsedExpenseImportRow row = toParsedRow(rowNumber, csvRow, fieldErrors);
                rows.add(row);
                if (!fieldErrors.isEmpty()) {
                    failedRows.add(new ImportRowFailure(rowNumber, summarize(fieldErrors), fieldErrors));
                }
                rowNumber++;
            }

            return new ParsedExpenseImportBatch(rows, failedRows);
        } catch (IOException ex) {
            throw new ValidationException("Unable to read the uploaded CSV file");
        }
    }

    private String readContent(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
                return content.substring(1);
            }
            return content;
        } catch (IOException ex) {
            throw new ValidationException("Unable to read the uploaded CSV file");
        }
    }

    private void validateHeaders(String content) {
        try {
            MappingIterator<Map<String, String>> iterator = csvMapper
                    .readerFor(new TypeReference<Map<String, String>>() {
                    })
                    .with(CsvSchema.emptySchema().withHeader())
                    .readValues(content);

            if (!iterator.hasNext()) {
                throw new ValidationException("CSV file contains no data rows");
            }

            Set<String> headers = iterator.next().keySet().stream()
                    .map(header -> header.trim().toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());

            if (!headers.containsAll(REQUIRED_HEADERS)) {
                throw new ValidationException(
                        "CSV header must contain: expense_date, amount, category, invoice_number, note");
            }
        } catch (IOException ex) {
            throw new ValidationException("Unable to read the uploaded CSV file");
        }
    }

    private Map<String, List<String>> validateRow(CsvExpenseUploadRow row) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<CsvExpenseUploadRow> violation : validator.validate(row)) {
            String fieldName = toColumnName(violation.getPropertyPath().toString());
            fieldErrors.computeIfAbsent(fieldName, ignored -> new ArrayList<>()).add(violation.getMessage());
        }
        if (!fieldErrors.containsKey(COL_EXPENSE_DATE)) {
            try {
                LocalDate.parse(row.expenseDate().trim());
            } catch (DateTimeParseException ex) {
                fieldErrors.computeIfAbsent(COL_EXPENSE_DATE, ignored -> new ArrayList<>())
                        .add("expense_date must be a valid calendar date");
            }
        }
        return fieldErrors;
    }

    private String toColumnName(String propertyName) {
        return switch (propertyName) {
            case "expenseDate" -> COL_EXPENSE_DATE;
            case "invoiceNumber" -> COL_INVOICE_NUMBER;
            default -> propertyName;
        };
    }

    private void addDuplicateInvoiceError(
            String invoiceNumber,
            int rowNumber,
            Map<String, Integer> invoiceRowNumbers,
            Map<String, List<String>> fieldErrors
    ) {
        String normalized = blankToNull(invoiceNumber);
        if (normalized == null) {
            return;
        }

        Integer firstRowNumber = invoiceRowNumbers.putIfAbsent(normalized, rowNumber);
        if (firstRowNumber != null) {
            fieldErrors.computeIfAbsent(COL_INVOICE_NUMBER, ignored -> new ArrayList<>())
                    .add("duplicate invoice_number within file; first seen at row " + firstRowNumber);
        }
    }

    private ParsedExpenseImportRow toParsedRow(
            int rowNumber,
            CsvExpenseUploadRow csvRow,
            Map<String, List<String>> fieldErrors
    ) {
        LocalDate expenseDate = fieldErrors.containsKey(COL_EXPENSE_DATE)
                ? null
                : LocalDate.parse(csvRow.expenseDate().trim());
        BigDecimal amount = fieldErrors.containsKey(COL_AMOUNT)
                ? null
                : new BigDecimal(csvRow.amount().trim());

        return new ParsedExpenseImportRow(
                rowNumber,
                expenseDate,
                amount,
                blankToNull(csvRow.category()),
                blankToNull(csvRow.invoiceNumber()),
                blankToNull(csvRow.note()));
    }

    private String summarize(Map<String, List<String>> fieldErrors) {
        return fieldErrors.entrySet().stream()
                .findFirst()
                .map(entry -> entry.getKey() + ": " + String.join("; ", entry.getValue()))
                .orElse("Row failed validation");
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private record CsvExpenseUploadRow(
            @JsonProperty(COL_EXPENSE_DATE)
            @NotBlank(message = "expense_date is required")
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "expense_date must use yyyy-MM-dd")
            String expenseDate,

            @JsonProperty(COL_AMOUNT)
            @NotBlank(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be greater than 0")
            String amount,

            @JsonProperty(COL_CATEGORY)
            @NotBlank(message = "category is required")
            @Size(max = 100, message = "category must be at most 100 characters")
            String category,

            @JsonProperty(COL_INVOICE_NUMBER)
            @Size(max = 20, message = "invoice_number must be at most 20 characters")
            String invoiceNumber,

            @JsonProperty(COL_NOTE)
            @Size(max = 500, message = "note must be at most 500 characters")
            String note
    ) {
    }
}
