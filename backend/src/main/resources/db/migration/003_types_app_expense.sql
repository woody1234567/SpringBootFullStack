-- Table-valued parameter type used by app_expense.import_expenses_batch for atomic CSV bulk import.
IF NOT EXISTS (
    SELECT 1 FROM sys.types t
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    WHERE s.name = N'app_expense' AND t.name = N'expense_import_row_type'
)
BEGIN
    CREATE TYPE app_expense.expense_import_row_type AS TABLE (
        row_number      int           NOT NULL,
        expense_date    date          NULL,
        amount          decimal(12,2) NULL,
        category_name   nvarchar(100) NULL,
        invoice_number  nvarchar(20)  NULL,
        note            nvarchar(500) NULL
    );
END;
GO
