-- app_expense.SP_GET_ACTIVE_CATEGORY: returns active categories for dropdowns.
CREATE OR REPLACE PROCEDURE app_expense.SP_GET_ACTIVE_CATEGORY (
    p_result_code      OUT VARCHAR2,
    p_result_message   OUT VARCHAR2,
    p_category_cursor  OUT SYS_REFCURSOR
)
AS
BEGIN
    OPEN p_category_cursor FOR
        SELECT category_id, name
        FROM app_expense.TB_CATEGORY
        WHERE is_active = 1
        ORDER BY name;

    p_result_code := 'SUCCESS';
    p_result_message := 'Categories found';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        OPEN p_category_cursor FOR
            SELECT category_id, name
            FROM app_expense.TB_CATEGORY
            WHERE 1 = 0;
        RAISE;
END SP_GET_ACTIVE_CATEGORY;
/
