-- Legacy collection type kept for compatibility with databases that already
-- applied the earlier array-based CSV import procedure. The current Java import
-- path uses JdbcTemplate.batchUpdate with app_expense.SP_INSERT_IMPORTED_EXPENSE.
CREATE OR REPLACE TYPE app_expense.TO_EXPENSE_IMPORT_ROW FORCE AS OBJECT (
    row_number      NUMBER(10),
    expense_date    DATE,
    amount          NUMBER(12,2),
    category_name   NVARCHAR2(100),
    invoice_number  VARCHAR2(20 CHAR),
    note            NVARCHAR2(500)
);
/

CREATE OR REPLACE TYPE app_expense.TT_EXPENSE_IMPORT_ROW AS TABLE OF app_expense.TO_EXPENSE_IMPORT_ROW;
/

-- Legacy session-scoped scratch table kept for compatibility with earlier
-- import procedures. New failed rows are persisted in TB_IMPORT_FAILED_ROW.
DECLARE
    v_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tables
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_TMP_IMPORT_FAILED_ROW';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE GLOBAL TEMPORARY TABLE app_expense.TB_TMP_IMPORT_FAILED_ROW (
                row_number    NUMBER(10) NOT NULL,
                error_message VARCHAR2(4000 CHAR) NOT NULL
            ) ON COMMIT PRESERVE ROWS';
    END IF;
END;
/
