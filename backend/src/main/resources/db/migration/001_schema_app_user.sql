-- Tables for user accounts (the app_user schema itself is created in 000_setup_schemas.sql).
DECLARE
    v_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tables
    WHERE owner = 'APP_USER' AND table_name = 'TB_USER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE app_user.TB_USER (
                user_id         NUMBER(19) GENERATED ALWAYS AS IDENTITY,
                email           VARCHAR2(320 CHAR) NOT NULL,
                password_hash   VARCHAR2(255 CHAR) NOT NULL,
                display_name    VARCHAR2(100 CHAR),
                is_active       NUMBER(1) DEFAULT 1 NOT NULL,
                created_at      TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                updated_at      TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                CONSTRAINT PK_USER PRIMARY KEY (user_id),
                CONSTRAINT UK_USER UNIQUE (email),
                CONSTRAINT CK_USER CHECK (is_active IN (0, 1))
            )';
    END IF;
END;
/

-- app_expense.TB_EXPENSE (002) declares a cross-schema FK to app_user.TB_USER.
GRANT REFERENCES, SELECT ON app_user.TB_USER TO app_expense;
