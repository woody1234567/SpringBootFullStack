-- app_expense.SP_CREATE_EXPENSE: inserts a single expense record owned by p_user_id.
CREATE OR REPLACE PROCEDURE app_expense.SP_CREATE_EXPENSE (
    p_user_id        IN  VARCHAR2,
    p_expense_date   IN  DATE,
    p_amount         IN  NUMBER,
    p_category_id    IN  VARCHAR2,
    p_invoice_number IN  VARCHAR2 DEFAULT NULL,
    p_note           IN  NVARCHAR2 DEFAULT NULL,
    p_expense_id     OUT VARCHAR2,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2
)
AS
    v_count PLS_INTEGER;
BEGIN
    IF p_amount IS NULL OR p_amount <= 0 THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'Amount must be greater than 0';
        RETURN;
    END IF;

    IF p_expense_date IS NULL THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'Expense date is required';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM app_expense.TB_CATEGORY
    WHERE category_id = p_category_id AND is_active = 1;

    IF v_count = 0 THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'Category not found';
        RETURN;
    END IF;

    IF p_invoice_number IS NOT NULL THEN
        SELECT COUNT(*) INTO v_count
        FROM app_expense.TB_EXPENSE
        WHERE user_id = p_user_id AND invoice_number = p_invoice_number;

        IF v_count > 0 THEN
            p_result_code := 'DUPLICATE';
            p_result_message := 'Invoice number already recorded';
            RETURN;
        END IF;
    END IF;

    INSERT INTO app_expense.TB_EXPENSE (user_id, category_id, expense_date, amount, invoice_number, note)
    VALUES (p_user_id, p_category_id, p_expense_date, p_amount, p_invoice_number, p_note)
    RETURNING expense_id INTO p_expense_id;

    p_result_code := 'SUCCESS';
    p_result_message := 'Expense created successfully';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        RAISE;
END SP_CREATE_EXPENSE;
/

-- app_expense.SP_UPDATE_EXPENSE: updates an expense, enforcing ownership by p_user_id.
CREATE OR REPLACE PROCEDURE app_expense.SP_UPDATE_EXPENSE (
    p_expense_id     IN  VARCHAR2,
    p_user_id        IN  VARCHAR2,
    p_expense_date   IN  DATE,
    p_amount         IN  NUMBER,
    p_category_id    IN  VARCHAR2,
    p_invoice_number IN  VARCHAR2 DEFAULT NULL,
    p_note           IN  NVARCHAR2 DEFAULT NULL,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2
)
AS
    v_count PLS_INTEGER;
BEGIN
    -- Row not found and row-belongs-to-another-user both report NOT_FOUND so a
    -- caller cannot distinguish "doesn't exist" from "not yours" (avoids ID enumeration).
    SELECT COUNT(*) INTO v_count
    FROM app_expense.TB_EXPENSE
    WHERE expense_id = p_expense_id AND user_id = p_user_id;

    IF v_count = 0 THEN
        p_result_code := 'NOT_FOUND';
        p_result_message := 'Expense not found';
        RETURN;
    END IF;

    IF p_amount IS NULL OR p_amount <= 0 THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'Amount must be greater than 0';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM app_expense.TB_CATEGORY
    WHERE category_id = p_category_id AND is_active = 1;

    IF v_count = 0 THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'Category not found';
        RETURN;
    END IF;

    IF p_invoice_number IS NOT NULL THEN
        SELECT COUNT(*) INTO v_count
        FROM app_expense.TB_EXPENSE
        WHERE user_id = p_user_id AND invoice_number = p_invoice_number AND expense_id <> p_expense_id;

        IF v_count > 0 THEN
            p_result_code := 'DUPLICATE';
            p_result_message := 'Invoice number already recorded';
            RETURN;
        END IF;
    END IF;

    UPDATE app_expense.TB_EXPENSE
    SET expense_date   = p_expense_date,
        amount         = p_amount,
        category_id    = p_category_id,
        invoice_number = p_invoice_number,
        note           = p_note,
        updated_at     = SYS_EXTRACT_UTC(SYSTIMESTAMP)
    WHERE expense_id = p_expense_id AND user_id = p_user_id;

    p_result_code := 'SUCCESS';
    p_result_message := 'Expense updated successfully';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        RAISE;
END SP_UPDATE_EXPENSE;
/

-- app_expense.SP_DELETE_EXPENSE: deletes an expense, enforcing ownership by p_user_id.
CREATE OR REPLACE PROCEDURE app_expense.SP_DELETE_EXPENSE (
    p_expense_id     IN  VARCHAR2,
    p_user_id        IN  VARCHAR2,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2
)
AS
    v_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM app_expense.TB_EXPENSE
    WHERE expense_id = p_expense_id AND user_id = p_user_id;

    IF v_count = 0 THEN
        p_result_code := 'NOT_FOUND';
        p_result_message := 'Expense not found';
        RETURN;
    END IF;

    DELETE FROM app_expense.TB_EXPENSE
    WHERE expense_id = p_expense_id AND user_id = p_user_id;

    p_result_code := 'SUCCESS';
    p_result_message := 'Expense deleted successfully';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        RAISE;
END SP_DELETE_EXPENSE;
/

-- app_expense.SP_GET_EXPENSE_DETAIL: fetches a single expense, enforcing ownership by p_user_id.
CREATE OR REPLACE PROCEDURE app_expense.SP_GET_EXPENSE_DETAIL (
    p_expense_id     IN  VARCHAR2,
    p_user_id        IN  VARCHAR2,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2,
    p_expense_cursor OUT SYS_REFCURSOR
)
AS
    v_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM app_expense.TB_EXPENSE
    WHERE expense_id = p_expense_id AND user_id = p_user_id;

    IF v_count = 0 THEN
        p_result_code := 'NOT_FOUND';
        p_result_message := 'Expense not found';
        OPEN p_expense_cursor FOR
            SELECT e.expense_id, e.expense_date, e.amount, e.category_id, c.name AS category_name,
                   e.invoice_number, e.note, e.created_at, e.updated_at
            FROM app_expense.TB_EXPENSE e
            JOIN app_expense.TB_CATEGORY c ON c.category_id = e.category_id
            WHERE 1 = 0;
        RETURN;
    END IF;

    OPEN p_expense_cursor FOR
        SELECT e.expense_id, e.expense_date, e.amount, e.category_id, c.name AS category_name,
               e.invoice_number, e.note, e.created_at, e.updated_at
        FROM app_expense.TB_EXPENSE e
        JOIN app_expense.TB_CATEGORY c ON c.category_id = e.category_id
        WHERE e.expense_id = p_expense_id AND e.user_id = p_user_id;

    p_result_code := 'SUCCESS';
    p_result_message := 'Expense found';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        RAISE;
END SP_GET_EXPENSE_DETAIL;
/

-- app_expense.SP_SEARCH_EXPENSE: paginated, filterable listing scoped to p_user_id.
CREATE OR REPLACE PROCEDURE app_expense.SP_SEARCH_EXPENSE (
    p_user_id        IN  VARCHAR2,
    p_date_from      IN  DATE DEFAULT NULL,
    p_date_to        IN  DATE DEFAULT NULL,
    p_category_id    IN  VARCHAR2 DEFAULT NULL,
    p_page           IN  NUMBER DEFAULT 1,
    p_page_size      IN  NUMBER DEFAULT 20,
    p_total_count    OUT NUMBER,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2,
    p_expense_cursor OUT SYS_REFCURSOR
)
AS
    v_page      NUMBER;
    v_page_size NUMBER;
BEGIN
    v_page := CASE WHEN p_page IS NULL OR p_page < 1 THEN 1 ELSE p_page END;
    v_page_size := CASE WHEN p_page_size IS NULL OR p_page_size < 1 THEN 20 ELSE p_page_size END;
    IF v_page_size > 100 THEN
        v_page_size := 100; -- clamp so a client cannot request unbounded pages
    END IF;

    SELECT COUNT(*) INTO p_total_count
    FROM app_expense.TB_EXPENSE e
    WHERE e.user_id = p_user_id
      AND (p_date_from IS NULL OR e.expense_date >= p_date_from)
      AND (p_date_to IS NULL OR e.expense_date <= p_date_to)
      AND (p_category_id IS NULL OR e.category_id = p_category_id);

    OPEN p_expense_cursor FOR
        SELECT e.expense_id, e.expense_date, e.amount, e.category_id, c.name AS category_name,
               e.invoice_number, e.note, e.created_at, e.updated_at
        FROM app_expense.TB_EXPENSE e
        JOIN app_expense.TB_CATEGORY c ON c.category_id = e.category_id
        WHERE e.user_id = p_user_id
          AND (p_date_from IS NULL OR e.expense_date >= p_date_from)
          AND (p_date_to IS NULL OR e.expense_date <= p_date_to)
          AND (p_category_id IS NULL OR e.category_id = p_category_id)
        ORDER BY e.expense_date DESC, e.expense_id DESC
        OFFSET (v_page - 1) * v_page_size ROWS FETCH NEXT v_page_size ROWS ONLY;

    p_result_code := 'SUCCESS';
    p_result_message := 'Search completed successfully';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        RAISE;
END SP_SEARCH_EXPENSE;
/
