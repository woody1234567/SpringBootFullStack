-- Reusable query logic for populating the category dropdown / validating category ids.
CREATE OR REPLACE VIEW app_expense.VW_ACTIVE_CATEGORY
AS
    SELECT category_id, name
    FROM app_expense.TB_CATEGORY
    WHERE is_active = 1;
