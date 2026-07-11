-- Reusable query logic for populating the category dropdown / validating category ids.
CREATE OR ALTER VIEW app_expense.v_active_categories
AS
    SELECT category_id, name
    FROM app_expense.categories
    WHERE is_active = 1;
GO
