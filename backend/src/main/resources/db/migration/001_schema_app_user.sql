-- Schema and tables for user accounts.
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'app_user')
    EXEC('CREATE SCHEMA app_user');
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'app_user.users'))
BEGIN
    CREATE TABLE app_user.users (
        user_id         bigint IDENTITY(1,1) NOT NULL,
        email           nvarchar(320)  NOT NULL,
        password_hash   nvarchar(255)  NOT NULL,
        display_name    nvarchar(100)  NULL,
        is_active       bit            NOT NULL CONSTRAINT DF_users_is_active DEFAULT (1),
        created_at      datetime2(3)   NOT NULL CONSTRAINT DF_users_created_at DEFAULT (SYSUTCDATETIME()),
        updated_at      datetime2(3)   NOT NULL CONSTRAINT DF_users_updated_at DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_users PRIMARY KEY CLUSTERED (user_id),
        CONSTRAINT UQ_users_email UNIQUE (email)
    );
END;
GO
