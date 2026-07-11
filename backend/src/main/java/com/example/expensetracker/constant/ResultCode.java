package com.example.expensetracker.constant;

/**
 * Result codes returned by SQL Server stored procedures via the
 * {@code @result_code} output parameter. Mirrors the convention documented in
 * .claude/CLAUDE.md section 7.
 */
public final class ResultCode {

    public static final String SUCCESS = "SUCCESS";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    private ResultCode() {
    }
}
