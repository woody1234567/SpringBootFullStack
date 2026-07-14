-- app_expense.SP_IMPORT_EXPENSE_BATCH: atomic CSV bulk import.
--
-- All rows are validated up front. If ANY row fails, nothing is inserted and the
-- failing rows are returned through p_failed_cursor. If every row passes, all
-- rows are inserted and committed together (database-owned transaction).
-- The TB_IMPORT_BATCH audit row is always written and committed, even for failed
-- imports, so an audit trail exists in every outcome.
--
-- Failed rows are staged in app_expense.TB_TMP_IMPORT_FAILED_ROW (session-scoped
-- global temporary table, ON COMMIT PRESERVE ROWS, created in 003) so they
-- survive the audit COMMIT and remain fetchable through the ref cursor; the
-- table is cleared at the start of every call.
CREATE OR REPLACE PROCEDURE app_expense.SP_IMPORT_EXPENSE_BATCH (
    p_user_id        IN  NUMBER,
    p_file_name      IN  VARCHAR2 DEFAULT NULL,
    p_rows           IN  app_expense.TT_EXPENSE_IMPORT_ROW,
    p_batch_id       OUT NUMBER,
    p_success_count  OUT NUMBER,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2,
    p_failed_cursor  OUT SYS_REFCURSOR
)
AS
    v_total_rows    PLS_INTEGER := 0;
    v_failed_rows   PLS_INTEGER := 0;
    v_error_summary VARCHAR2(4000);
BEGIN
    IF p_rows IS NOT NULL THEN
        v_total_rows := p_rows.COUNT;
    END IF;
    p_success_count := 0;

    DELETE FROM app_expense.TB_TMP_IMPORT_FAILED_ROW;

    IF v_total_rows = 0 THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'CSV file contains no data rows';

        INSERT INTO app_expense.TB_IMPORT_BATCH (user_id, file_name, total_rows, success_rows, status, error_summary)
        VALUES (p_user_id, p_file_name, 0, 0, 'FAILED', p_result_message)
        RETURNING batch_id INTO p_batch_id;
        COMMIT;

        OPEN p_failed_cursor FOR
            SELECT f.row_number, f.error_message
            FROM app_expense.TB_TMP_IMPORT_FAILED_ROW f
            WHERE 1 = 0;
        RETURN;
    END IF;

    -- Collect every failing row in one pass (first applicable reason per row).
    INSERT INTO app_expense.TB_TMP_IMPORT_FAILED_ROW (row_number, error_message)
    SELECT x.row_number, x.error_message
    FROM (
        SELECT
            r.row_number,
            CASE
                WHEN r.expense_date IS NULL
                    THEN 'Row ' || TO_CHAR(r.row_number) || ': invalid or missing expense_date'
                WHEN r.amount IS NULL OR r.amount <= 0
                    THEN 'Row ' || TO_CHAR(r.row_number) || ': amount must be greater than 0'
                WHEN r.category_name IS NULL OR NOT EXISTS (
                    SELECT 1 FROM app_expense.TB_CATEGORY c
                    WHERE c.is_active = 1 AND UPPER(c.name) = UPPER(r.category_name)
                )
                    THEN 'Row ' || TO_CHAR(r.row_number) || ': category not found'
                WHEN r.invoice_number IS NOT NULL AND EXISTS (
                    SELECT 1 FROM TABLE(p_rows) r2
                    WHERE r2.invoice_number = r.invoice_number AND r2.row_number <> r.row_number
                )
                    THEN 'Row ' || TO_CHAR(r.row_number) || ': duplicate invoice_number within file'
                WHEN r.invoice_number IS NOT NULL AND EXISTS (
                    SELECT 1 FROM app_expense.TB_EXPENSE e
                    WHERE e.user_id = p_user_id AND e.invoice_number = r.invoice_number
                )
                    THEN 'Row ' || TO_CHAR(r.row_number) || ': invoice_number already recorded'
                ELSE NULL
            END AS error_message
        FROM TABLE(p_rows) r
    ) x
    WHERE x.error_message IS NOT NULL;

    v_failed_rows := SQL%ROWCOUNT;

    IF v_failed_rows > 0 THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'One or more rows failed validation; no rows were imported';

        SELECT f.error_message INTO v_error_summary
        FROM app_expense.TB_TMP_IMPORT_FAILED_ROW f
        ORDER BY f.row_number
        FETCH FIRST 1 ROW ONLY;

        INSERT INTO app_expense.TB_IMPORT_BATCH (user_id, file_name, total_rows, success_rows, status, error_summary)
        VALUES (p_user_id, p_file_name, v_total_rows, 0, 'FAILED', v_error_summary)
        RETURNING batch_id INTO p_batch_id;
        COMMIT;

        OPEN p_failed_cursor FOR
            SELECT f.row_number, f.error_message
            FROM app_expense.TB_TMP_IMPORT_FAILED_ROW f
            ORDER BY f.row_number;
        RETURN;
    END IF;

    INSERT INTO app_expense.TB_EXPENSE (user_id, category_id, expense_date, amount, invoice_number, note)
    SELECT p_user_id, c.category_id, r.expense_date, r.amount, r.invoice_number, r.note
    FROM TABLE(p_rows) r
    JOIN app_expense.TB_CATEGORY c ON UPPER(c.name) = UPPER(r.category_name) AND c.is_active = 1;

    p_success_count := SQL%ROWCOUNT;

    p_result_code := 'SUCCESS';
    p_result_message := 'Import completed successfully';

    INSERT INTO app_expense.TB_IMPORT_BATCH (user_id, file_name, total_rows, success_rows, status, error_summary)
    VALUES (p_user_id, p_file_name, v_total_rows, p_success_count, 'SUCCESS', NULL)
    RETURNING batch_id INTO p_batch_id;

    COMMIT;

    OPEN p_failed_cursor FOR
        SELECT f.row_number, f.error_message
        FROM app_expense.TB_TMP_IMPORT_FAILED_ROW f
        WHERE 1 = 0; -- empty failed-rows result set
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;

        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        p_success_count := 0;

        INSERT INTO app_expense.TB_IMPORT_BATCH (user_id, file_name, total_rows, success_rows, status, error_summary)
        VALUES (p_user_id, p_file_name, v_total_rows, 0, 'FAILED', 'System error during import')
        RETURNING batch_id INTO p_batch_id;
        COMMIT;

        RAISE;
END SP_IMPORT_EXPENSE_BATCH;
/
