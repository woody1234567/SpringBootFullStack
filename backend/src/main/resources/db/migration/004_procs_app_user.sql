-- app_user.create_user: registers a new user account.
CREATE OR ALTER PROCEDURE app_user.create_user
    @email          nvarchar(320),
    @password_hash  nvarchar(255),
    @display_name   nvarchar(100) = NULL,
    @user_id        bigint OUTPUT,
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        IF @email IS NULL OR LEN(LTRIM(RTRIM(@email))) = 0
           OR @password_hash IS NULL OR LEN(@password_hash) = 0
        BEGIN
            SET @result_code = N'VALIDATION_ERROR';
            SET @result_message = N'Email and password are required';
            RETURN;
        END;

        IF EXISTS (SELECT 1 FROM app_user.users WHERE email = @email)
        BEGIN
            SET @result_code = N'DUPLICATE';
            SET @result_message = N'Email already exists';
            RETURN;
        END;

        INSERT INTO app_user.users (email, password_hash, display_name)
        VALUES (@email, @password_hash, @display_name);

        SET @user_id = CONVERT(bigint, SCOPE_IDENTITY());
        SET @result_code = N'SUCCESS';
        SET @result_message = N'User created successfully';
    END TRY
    BEGIN CATCH
        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        THROW;
    END CATCH;
END;
GO

-- app_user.get_user_by_email: looks up an account (including password hash) for login verification.
CREATE OR ALTER PROCEDURE app_user.get_user_by_email
    @email          nvarchar(320),
    @result_code    nvarchar(50) OUTPUT,
    @result_message nvarchar(4000) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM app_user.users WHERE email = @email)
        BEGIN
            SET @result_code = N'NOT_FOUND';
            SET @result_message = N'User not found';
            RETURN;
        END;

        SELECT user_id, email, password_hash, display_name, is_active
        FROM app_user.users
        WHERE email = @email;

        SET @result_code = N'SUCCESS';
        SET @result_message = N'User found';
    END TRY
    BEGIN CATCH
        SET @result_code = N'SYSTEM_ERROR';
        SET @result_message = N'Unable to complete the database operation';
        THROW;
    END CATCH;
END;
GO
