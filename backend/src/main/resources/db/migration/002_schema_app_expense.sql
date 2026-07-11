-- Schema and tables for expense tracking (categories, expenses, import batches).
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'app_expense')
    EXEC('CREATE SCHEMA app_expense');
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'app_expense.categories'))
BEGIN
    CREATE TABLE app_expense.categories (
        category_id     int IDENTITY(1,1) NOT NULL,
        name            nvarchar(100) NOT NULL,
        is_active       bit           NOT NULL CONSTRAINT DF_categories_is_active DEFAULT (1),
        CONSTRAINT PK_categories PRIMARY KEY CLUSTERED (category_id),
        CONSTRAINT UQ_categories_name UNIQUE (name)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM app_expense.categories)
BEGIN
    INSERT INTO app_expense.categories (name) VALUES
        (N'餐飲'), (N'交通'), (N'住宿'), (N'娛樂'), (N'醫療'), (N'其他');
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'app_expense.expenses'))
BEGIN
    CREATE TABLE app_expense.expenses (
        expense_id      bigint IDENTITY(1,1) NOT NULL,
        user_id         bigint NOT NULL,
        category_id     int    NOT NULL,
        expense_date    date   NOT NULL,
        amount          decimal(12,2) NOT NULL,
        invoice_number  nvarchar(20)  NULL,
        note            nvarchar(500) NULL,
        created_at      datetime2(3) NOT NULL CONSTRAINT DF_expenses_created_at DEFAULT (SYSUTCDATETIME()),
        updated_at      datetime2(3) NOT NULL CONSTRAINT DF_expenses_updated_at DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_expenses PRIMARY KEY CLUSTERED (expense_id),
        CONSTRAINT FK_expenses_user FOREIGN KEY (user_id) REFERENCES app_user.users (user_id),
        CONSTRAINT FK_expenses_category FOREIGN KEY (category_id) REFERENCES app_expense.categories (category_id),
        CONSTRAINT CK_expenses_amount_positive CHECK (amount > 0),
        -- NOTE: unlike Postgres, SQL Server's plain UNIQUE CONSTRAINT treats all NULLs in a
        -- column as equal, so this only allows ONE no-invoice expense per user. Corrected in
        -- 008_fix_expenses_invoice_null_unique.sql, which replaces this with a filtered unique
        -- index (WHERE invoice_number IS NOT NULL). Kept here unchanged for migration history.
        CONSTRAINT UQ_expenses_user_invoice UNIQUE (user_id, invoice_number)
    );

    CREATE NONCLUSTERED INDEX IX_expenses_user_date ON app_expense.expenses (user_id, expense_date DESC);
    CREATE NONCLUSTERED INDEX IX_expenses_user_category ON app_expense.expenses (user_id, category_id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'app_expense.import_batches'))
BEGIN
    CREATE TABLE app_expense.import_batches (
        batch_id        bigint IDENTITY(1,1) NOT NULL,
        user_id         bigint NOT NULL,
        file_name       nvarchar(255) NULL,
        total_rows      int NOT NULL,
        success_rows    int NOT NULL,
        status          nvarchar(20) NOT NULL,
        error_summary   nvarchar(4000) NULL,
        created_at      datetime2(3) NOT NULL CONSTRAINT DF_import_batches_created_at DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_import_batches PRIMARY KEY CLUSTERED (batch_id),
        CONSTRAINT FK_import_batches_user FOREIGN KEY (user_id) REFERENCES app_user.users (user_id)
    );
END;
GO
