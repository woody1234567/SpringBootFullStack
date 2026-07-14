package com.example.expensetracker.constant;

/**
 * Shared Oracle stored procedure parameter names. Spring JDBC map keys must
 * match the PL/SQL parameter names exactly.
 */
public final class SqlParamNames {

    public static final String USER_ID = "p_user_id";
    public static final String EMAIL = "p_email";
    public static final String PASSWORD_HASH = "p_password_hash";
    public static final String DISPLAY_NAME = "p_display_name";

    public static final String EXPENSE_ID = "p_expense_id";
    public static final String EXPENSE_DATE = "p_expense_date";
    public static final String AMOUNT = "p_amount";
    public static final String CATEGORY_ID = "p_category_id";
    public static final String INVOICE_NUMBER = "p_invoice_number";
    public static final String NOTE = "p_note";
    public static final String DATE_FROM = "p_date_from";
    public static final String DATE_TO = "p_date_to";
    public static final String PAGE = "p_page";
    public static final String PAGE_SIZE = "p_page_size";
    public static final String TOTAL_COUNT = "p_total_count";

    public static final String FILE_NAME = "p_file_name";
    public static final String ROWS = "p_rows";
    public static final String BATCH_ID = "p_batch_id";
    public static final String SUCCESS_COUNT = "p_success_count";

    public static final String RESULT_CODE = "p_result_code";
    public static final String RESULT_MESSAGE = "p_result_message";

    public static final String USER_CURSOR = "p_user_cursor";
    public static final String EXPENSE_CURSOR = "p_expense_cursor";
    public static final String FAILED_CURSOR = "p_failed_cursor";

    private SqlParamNames() {
    }
}
