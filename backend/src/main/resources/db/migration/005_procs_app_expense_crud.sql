-- app_expense.create_expense: inserts a single expense record owned by @user_id.
CREATE OR ALTER PROCEDURE app_expense.create_expense
    @user_id        bigint,
    @expense_date   date,
    @amount         decimal(12,2),
    @category_id    int,
    @invoice_number nvarchar(20)  = NULL,
    @note           nvarchar(500) = NULL,
    @expense_id     bigint OUTPUT,
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        IF @amount IS NULL OR @amount <= 0
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'Amount must be greater than 0';
            RETURN;
        END;

        IF @expense_date IS NULL
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'Expense date is required';
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM app_expense.categories WHERE category_id = @category_id AND is_active = 1)
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'Category not found';
            RETURN;
        END;

        IF @invoice_number IS NOT NULL AND EXISTS (
            SELECT 1 FROM app_expense.expenses
            WHERE user_id = @user_id AND invoice_number = @invoice_number
        )
        BEGIN
            SET @result_code = N'DUPLICATE';
            SET @result_message = N'Invoice number already recorded';
            RETURN;
        END;

        INSERT INTO app_expense.expenses (user_id, category_id, expense_date, amount, invoice_number, note)
        VALUES (@user_id, @category_id, @expense_date, @amount, @invoice_number, @note);

        SET @expense_id = CONVERT(bigint, SCOPE_IDENTITY());
        SET @result_code = N'SUCCESS';
        SET @result_message = N'Expense created successfully';
    END TRY
    BEGIN CATCH
        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        THROW;
    END CATCH;
END;
GO

-- app_expense.update_expense: updates an expense, enforcing ownership by @user_id.
CREATE OR ALTER PROCEDURE app_expense.update_expense
    @expense_id     bigint,
    @user_id        bigint,
    @expense_date   date,
    @amount         decimal(12,2),
    @category_id    int,
    @invoice_number nvarchar(20)  = NULL,
    @note           nvarchar(500) = NULL,
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        -- Row not found and row-belongs-to-another-user both report NOT_FOUND so a
        -- caller cannot distinguish "doesn't exist" from "not yours" (avoids ID enumeration).
        IF NOT EXISTS (SELECT 1 FROM app_expense.expenses WHERE expense_id = @expense_id AND user_id = @user_id)
        BEGIN
            SET @result_code = N'NOT_FOUND';
            SET @result_message = N'Expense not found';
            RETURN;
        END;

        IF @amount IS NULL OR @amount <= 0
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'Amount must be greater than 0';
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM app_expense.categories WHERE category_id = @category_id AND is_active = 1)
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'Category not found';
            RETURN;
        END;

        IF @invoice_number IS NOT NULL AND EXISTS (
            SELECT 1 FROM app_expense.expenses
            WHERE user_id = @user_id AND invoice_number = @invoice_number AND expense_id <> @expense_id
        )
        BEGIN
            SET @result_code = N'DUPLICATE';
            SET @result_message = N'Invoice number already recorded';
            RETURN;
        END;

        UPDATE app_expense.expenses
        SET expense_date   = @expense_date,
            amount         = @amount,
            category_id    = @category_id,
            invoice_number = @invoice_number,
            note           = @note,
            updated_at     = SYSUTCDATETIME()
        WHERE expense_id = @expense_id AND user_id = @user_id;

        SET @result_code = N'SUCCESS';
        SET @result_message = N'Expense updated successfully';
    END TRY
    BEGIN CATCH
        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        THROW;
    END CATCH;
END;
GO

-- app_expense.delete_expense: deletes an expense, enforcing ownership by @user_id.
CREATE OR ALTER PROCEDURE app_expense.delete_expense
    @expense_id     bigint,
    @user_id        bigint,
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM app_expense.expenses WHERE expense_id = @expense_id AND user_id = @user_id)
        BEGIN
            SET @result_code = N'NOT_FOUND';
            SET @result_message = N'Expense not found';
            RETURN;
        END;

        DELETE FROM app_expense.expenses WHERE expense_id = @expense_id AND user_id = @user_id;

        SET @result_code = N'SUCCESS';
        SET @result_message = N'Expense deleted successfully';
    END TRY
    BEGIN CATCH
        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        THROW;
    END CATCH;
END;
GO

-- app_expense.get_expense_detail: fetches a single expense, enforcing ownership by @user_id.
CREATE OR ALTER PROCEDURE app_expense.get_expense_detail
    @expense_id     bigint,
    @user_id        bigint,
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM app_expense.expenses WHERE expense_id = @expense_id AND user_id = @user_id)
        BEGIN
            SET @result_code = N'NOT_FOUND';
            SET @result_message = N'Expense not found';
            RETURN;
        END;

        SELECT e.expense_id, e.expense_date, e.amount, e.category_id, c.name AS category_name,
               e.invoice_number, e.note, e.created_at, e.updated_at
        FROM app_expense.expenses e
        JOIN app_expense.categories c ON c.category_id = e.category_id
        WHERE e.expense_id = @expense_id AND e.user_id = @user_id;

        SET @result_code = N'SUCCESS';
        SET @result_message = N'Expense found';
    END TRY
    BEGIN CATCH
        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        THROW;
    END CATCH;
END;
GO

-- app_expense.search_expenses: paginated, filterable listing scoped to @user_id.
CREATE OR ALTER PROCEDURE app_expense.search_expenses
    @user_id        bigint,
    @date_from      date = NULL,
    @date_to        date = NULL,
    @category_id    int  = NULL,
    @page           int  = 1,
    @page_size      int  = 20,
    @total_count    int OUTPUT,
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        IF @page IS NULL OR @page < 1 SET @page = 1;
        IF @page_size IS NULL OR @page_size < 1 SET @page_size = 20;
        IF @page_size > 100 SET @page_size = 100; -- clamp so a client cannot request unbounded pages

        SELECT @total_count = COUNT(*)
        FROM app_expense.expenses e
        WHERE e.user_id = @user_id
          AND (@date_from IS NULL OR e.expense_date >= @date_from)
          AND (@date_to IS NULL OR e.expense_date <= @date_to)
          AND (@category_id IS NULL OR e.category_id = @category_id);

        SELECT e.expense_id, e.expense_date, e.amount, e.category_id, c.name AS category_name,
               e.invoice_number, e.note, e.created_at, e.updated_at
        FROM app_expense.expenses e
        JOIN app_expense.categories c ON c.category_id = e.category_id
        WHERE e.user_id = @user_id
          AND (@date_from IS NULL OR e.expense_date >= @date_from)
          AND (@date_to IS NULL OR e.expense_date <= @date_to)
          AND (@category_id IS NULL OR e.category_id = @category_id)
        ORDER BY e.expense_date DESC, e.expense_id DESC
        OFFSET (@page - 1) * @page_size ROWS FETCH NEXT @page_size ROWS ONLY;

        SET @result_code = N'SUCCESS';
        SET @result_message = N'Search completed successfully';
    END TRY
    BEGIN CATCH
        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        THROW;
    END CATCH;
END;
GO
