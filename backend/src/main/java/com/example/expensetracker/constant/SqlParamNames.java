package com.example.expensetracker.constant;

/**
 * Shared SQL Server stored procedure parameter names (declared without the
 * leading {@code @}, per .claude/CLAUDE.md section 6).
 */
public final class SqlParamNames {

    public static final String USER_ID = "user_id";
    public static final String EMAIL = "email";
    public static final String PASSWORD_HASH = "password_hash";
    public static final String DISPLAY_NAME = "display_name";

    public static final String EXPENSE_ID = "expense_id";
    public static final String EXPENSE_DATE = "expense_date";
    public static final String AMOUNT = "amount";
    public static final String CATEGORY_ID = "category_id";
    public static final String INVOICE_NUMBER = "invoice_number";
    public static final String NOTE = "note";
    public static final String DATE_FROM = "date_from";
    public static final String DATE_TO = "date_to";
    public static final String PAGE = "page";
    public static final String PAGE_SIZE = "page_size";
    public static final String TOTAL_COUNT = "total_count";

    public static final String FILE_NAME = "file_name";
    public static final String ROWS = "rows";
    public static final String BATCH_ID = "batch_id";
    public static final String SUCCESS_COUNT = "success_count";

    public static final String RESULT_CODE = "result_code";
    public static final String RESULT_MESSAGE = "result_message";

    private SqlParamNames() {
    }
}
