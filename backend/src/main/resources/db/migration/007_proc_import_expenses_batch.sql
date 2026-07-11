-- app_expense.import_expenses_batch: atomic CSV bulk import.
--
-- All rows are validated up front. If ANY row fails, nothing is inserted (whole
-- batch rolls back) and the failing rows are returned as a second result set.
-- If every row passes, all rows are inserted in a single transaction.
-- The import_batches audit row is always written, after the rows transaction has
-- resolved (commit or rollback), so an audit trail exists even for failed imports.
CREATE OR ALTER PROCEDURE app_expense.import_expenses_batch
    @user_id        bigint,
    @file_name      nvarchar(255) = NULL,
    @rows           app_expense.expense_import_row_type READONLY,
    @batch_id       bigint OUTPUT,
    @success_count  int OUTPUT,
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @total_rows int;
    SELECT @total_rows = COUNT(*) FROM @rows;
    SET @success_count = 0;

    CREATE TABLE #failed_rows (
        row_number    int NOT NULL,
        error_message nvarchar(4000) NOT NULL
    );

    BEGIN TRY
        IF @total_rows IS NULL OR @total_rows = 0
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'CSV file contains no data rows';

            INSERT INTO app_expense.import_batches (user_id, file_name, total_rows, success_rows, status, error_summary)
            VALUES (@user_id, @file_name, 0, 0, N'FAILED', @result_message);
            SET @batch_id = CONVERT(bigint, SCOPE_IDENTITY());
            RETURN;
        END;

        -- Collect every failing row in one pass (first applicable reason per row).
        INSERT INTO #failed_rows (row_number, error_message)
        SELECT x.row_number, x.error_message
        FROM (
            SELECT
                r.row_number,
                CASE
                    WHEN r.expense_date IS NULL
                        THEN N'Row ' + CONVERT(nvarchar(10), r.row_number) + N': invalid or missing expense_date'
                    WHEN r.amount IS NULL OR r.amount <= 0
                        THEN N'Row ' + CONVERT(nvarchar(10), r.row_number) + N': amount must be greater than 0'
                    WHEN r.category_name IS NULL OR NOT EXISTS (
                        SELECT 1 FROM app_expense.categories c
                        WHERE c.is_active = 1 AND UPPER(c.name) = UPPER(r.category_name)
                    )
                        THEN N'Row ' + CONVERT(nvarchar(10), r.row_number) + N': category not found'
                    WHEN r.invoice_number IS NOT NULL AND EXISTS (
                        SELECT 1 FROM @rows r2
                        WHERE r2.invoice_number = r.invoice_number AND r2.row_number <> r.row_number
                    )
                        THEN N'Row ' + CONVERT(nvarchar(10), r.row_number) + N': duplicate invoice_number within file'
                    WHEN r.invoice_number IS NOT NULL AND EXISTS (
                        SELECT 1 FROM app_expense.expenses e
                        WHERE e.user_id = @user_id AND e.invoice_number = r.invoice_number
                    )
                        THEN N'Row ' + CONVERT(nvarchar(10), r.row_number) + N': invoice_number already recorded'
                    ELSE NULL
                END AS error_message
            FROM @rows r
        ) x
        WHERE x.error_message IS NOT NULL;

        IF EXISTS (SELECT 1 FROM #failed_rows)
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'One or more rows failed validation; no rows were imported';

            INSERT INTO app_expense.import_batches (user_id, file_name, total_rows, success_rows, status, error_summary)
            VALUES (@user_id, @file_name, @total_rows, 0, N'FAILED',
                    (SELECT TOP (1) error_message FROM #failed_rows ORDER BY row_number));
            SET @batch_id = CONVERT(bigint, SCOPE_IDENTITY());

            SELECT row_number, error_message FROM #failed_rows ORDER BY row_number;
            RETURN;
        END;

        BEGIN TRANSACTION;

        INSERT INTO app_expense.expenses (user_id, category_id, expense_date, amount, invoice_number, note)
        SELECT @user_id, c.category_id, r.expense_date, r.amount, r.invoice_number, r.note
        FROM @rows r
        JOIN app_expense.categories c ON UPPER(c.name) = UPPER(r.category_name) AND c.is_active = 1;

        SET @success_count = @@ROWCOUNT;

        COMMIT TRANSACTION;

        SET @result_code = N'SUCCESS';
        SET @result_message = N'Import completed successfully';

        INSERT INTO app_expense.import_batches (user_id, file_name, total_rows, success_rows, status, error_summary)
        VALUES (@user_id, @file_name, @total_rows, @success_count, N'SUCCESS', NULL);
        SET @batch_id = CONVERT(bigint, SCOPE_IDENTITY());

        SELECT TOP (0) CONVERT(int, NULL) AS row_number, CONVERT(nvarchar(4000), NULL) AS error_message; -- empty failed-rows result set
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0
            ROLLBACK TRANSACTION;

        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        SET @success_count = 0;

        INSERT INTO app_expense.import_batches (user_id, file_name, total_rows, success_rows, status, error_summary)
        VALUES (@user_id, @file_name, @total_rows, 0, N'FAILED', N'System error during import');
        SET @batch_id = CONVERT(bigint, SCOPE_IDENTITY());

        THROW;
    END CATCH;
END;
GO
