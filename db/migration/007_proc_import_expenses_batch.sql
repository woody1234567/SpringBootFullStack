-- CSV import procedures. Spring owns transaction boundaries: these procedures
-- do not COMMIT or ROLLBACK.
CREATE OR REPLACE PROCEDURE app_expense.SP_CREATE_IMPORT_BATCH (
    p_user_id        IN  VARCHAR2,
    p_file_name      IN  NVARCHAR2 DEFAULT NULL,
    p_total_rows     IN  NUMBER,
    p_batch_id       OUT VARCHAR2,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2
)
AS
BEGIN
    INSERT INTO app_expense.TB_IMPORT_BATCH (user_id, file_name, total_rows, success_rows, status, error_summary)
    VALUES (p_user_id, p_file_name, NVL(p_total_rows, 0), 0, 'PROCESSING', NULL)
    RETURNING batch_id INTO p_batch_id;

    p_result_code := 'SUCCESS';
    p_result_message := 'Import batch created';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to create import batch';
        RAISE;
END SP_CREATE_IMPORT_BATCH;
/

CREATE OR REPLACE PROCEDURE app_expense.SP_UPDATE_IMPORT_BATCH (
    p_batch_id       IN  VARCHAR2,
    p_success_count  IN  NUMBER,
    p_status         IN  VARCHAR2,
    p_error_summary  IN  NVARCHAR2 DEFAULT NULL,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2
)
AS
BEGIN
    IF p_status NOT IN ('SUCCESS', 'FAILED') THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'Import batch status must be SUCCESS or FAILED';
        RETURN;
    END IF;

    UPDATE app_expense.TB_IMPORT_BATCH
    SET success_rows = NVL(p_success_count, 0),
        status = p_status,
        error_summary = p_error_summary
    WHERE batch_id = p_batch_id;

    IF SQL%ROWCOUNT = 0 THEN
        p_result_code := 'NOT_FOUND';
        p_result_message := 'Import batch not found';
        RETURN;
    END IF;

    p_result_code := 'SUCCESS';
    p_result_message := 'Import batch updated';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to update import batch';
        RAISE;
END SP_UPDATE_IMPORT_BATCH;
/

CREATE OR REPLACE PROCEDURE app_expense.SP_CREATE_IMPORT_FAILED_ROW (
    p_batch_id       IN  VARCHAR2,
    p_row_number     IN  NUMBER,
    p_error_message  IN  VARCHAR2
)
AS
BEGIN
    INSERT INTO app_expense.TB_IMPORT_FAILED_ROW (batch_id, row_number, error_message)
    VALUES (p_batch_id, NVL(p_row_number, 0), SUBSTR(p_error_message, 1, 4000));
END SP_CREATE_IMPORT_FAILED_ROW;
/

CREATE OR REPLACE PROCEDURE app_expense.SP_INSERT_IMPORTED_EXPENSE (
    p_user_id         IN VARCHAR2,
    p_batch_id        IN VARCHAR2,
    p_row_number      IN NUMBER,
    p_expense_date    IN DATE,
    p_amount          IN NUMBER,
    p_category_name   IN NVARCHAR2,
    p_invoice_number  IN VARCHAR2 DEFAULT NULL,
    p_note            IN NVARCHAR2 DEFAULT NULL
)
AS
    v_category_id app_expense.TB_CATEGORY.category_id%TYPE;
BEGIN
    IF p_expense_date IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Row ' || p_row_number || ': expense_date is required');
    END IF;

    IF p_amount IS NULL OR p_amount <= 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Row ' || p_row_number || ': amount must be greater than 0');
    END IF;

    BEGIN
        SELECT c.category_id
        INTO v_category_id
        FROM app_expense.TB_CATEGORY c
        WHERE c.is_active = 1
          AND UPPER(c.name) = UPPER(p_category_name);
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Row ' || p_row_number || ': category not found');
        WHEN TOO_MANY_ROWS THEN
            RAISE_APPLICATION_ERROR(-20004, 'Row ' || p_row_number || ': category is ambiguous');
    END;

    INSERT INTO app_expense.TB_EXPENSE (
        user_id, batch_id, category_id, expense_date, amount, invoice_number, note
    )
    VALUES (
        p_user_id,
        p_batch_id,
        v_category_id,
        p_expense_date,
        p_amount,
        NULLIF(TRIM(p_invoice_number), ''),
        NULLIF(TRIM(p_note), '')
    );
EXCEPTION
    WHEN DUP_VAL_ON_INDEX THEN
        RAISE_APPLICATION_ERROR(-20005, 'Row ' || p_row_number || ': invoice_number already recorded');
END SP_INSERT_IMPORTED_EXPENSE;
/
