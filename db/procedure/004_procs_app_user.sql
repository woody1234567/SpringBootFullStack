-- app_user.SP_CREATE_USER: registers a new user account.
CREATE OR REPLACE PROCEDURE app_user.SP_CREATE_USER (
    p_email          IN  VARCHAR2,
    p_password_hash  IN  VARCHAR2,
    p_display_name   IN  NVARCHAR2 DEFAULT NULL,
    p_user_id        OUT VARCHAR2,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2
)
AS
    v_count PLS_INTEGER;
    v_user_id app_user.TB_USER.user_id%TYPE;
BEGIN
    -- In Oracle the empty string is NULL, so IS NULL also rejects '' inputs.
    IF p_email IS NULL OR TRIM(p_email) IS NULL OR p_password_hash IS NULL THEN
        p_result_code := 'VALIDATION_ERROR';
        p_result_message := 'Email and password are required';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM app_user.TB_USER
    WHERE email = p_email;

    IF v_count > 0 THEN
        p_result_code := 'DUPLICATE';
        p_result_message := 'Email already exists';
        RETURN;
    END IF;

    v_user_id := RAWTOHEX(SYS_GUID());

    INSERT INTO app_user.TB_USER (user_id, email, password_hash, display_name, creator, updater)
    VALUES (v_user_id, p_email, p_password_hash, p_display_name, v_user_id, v_user_id);

    p_user_id := v_user_id;

    p_result_code := 'SUCCESS';
    p_result_message := 'User created successfully';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        RAISE;
END SP_CREATE_USER;
/

-- app_user.SP_GET_USER_BY_EMAIL: looks up an account (including password hash) for login verification.
CREATE OR REPLACE PROCEDURE app_user.SP_GET_USER_BY_EMAIL (
    p_email          IN  VARCHAR2,
    p_result_code    OUT VARCHAR2,
    p_result_message OUT VARCHAR2,
    p_user_cursor    OUT SYS_REFCURSOR
)
AS
    v_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM app_user.TB_USER
    WHERE email = p_email;

    IF v_count = 0 THEN
        p_result_code := 'NOT_FOUND';
        p_result_message := 'User not found';
        -- Always hand back an opened (empty) cursor so the caller never fetches
        -- from an unopened ref cursor.
        OPEN p_user_cursor FOR
            SELECT user_id, email, password_hash, display_name, role, is_active
            FROM app_user.TB_USER
            WHERE 1 = 0;
        RETURN;
    END IF;

    OPEN p_user_cursor FOR
        SELECT user_id, email, password_hash, display_name, role, is_active
        FROM app_user.TB_USER
        WHERE email = p_email;

    p_result_code := 'SUCCESS';
    p_result_message := 'User found';
EXCEPTION
    WHEN OTHERS THEN
        p_result_code := 'SYSTEM_ERROR';
        p_result_message := 'Unable to complete the database operation';
        RAISE;
END SP_GET_USER_BY_EMAIL;
/
