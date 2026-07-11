-- Fixes app_expense.expenses: SQL Server's plain UNIQUE CONSTRAINT treats all NULLs in a
-- column as equal for uniqueness purposes (unlike Postgres), so UQ_expenses_user_invoice
-- only ever allowed ONE expense per user with no invoice number -- every second blank-invoice
-- expense failed with a raw DuplicateKeyException. A filtered unique index restores the
-- intended behavior: uniqueness is enforced only when an invoice number is actually present.
IF EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = N'UQ_expenses_user_invoice' AND parent_object_id = OBJECT_ID(N'app_expense.expenses')
)
BEGIN
    ALTER TABLE app_expense.expenses DROP CONSTRAINT UQ_expenses_user_invoice;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'UQ_expenses_user_invoice' AND object_id = OBJECT_ID(N'app_expense.expenses')
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_expenses_user_invoice
        ON app_expense.expenses (user_id, invoice_number)
        WHERE invoice_number IS NOT NULL;
END;
GO
