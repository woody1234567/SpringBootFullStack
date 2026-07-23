-- Collection type used by app_expense.SP_IMPORT_EXPENSE_BATCH for atomic CSV bulk
-- import (Oracle equivalent of the SQL Server table-valued parameter): a
-- schema-level object type plus a nested table of it, passed as an IN parameter
-- and queried with TABLE(...).
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

-- Session-scoped scratch table (global temporary table) replacing the SQL Server
-- #failed_rows temp table: SP_IMPORT_EXPENSE_BATCH collects validation failures
-- here and returns them through a ref cursor. ON COMMIT PRESERVE ROWS so the
-- rows remain fetchable while Spring consumes the ref cursor; the procedure
-- clears the table at the start of every call.
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
