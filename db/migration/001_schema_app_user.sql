-- Tables for user accounts (the app_user schema itself is created in 000_setup_schemas.sql).
DECLARE
    v_count            PLS_INTEGER;
    v_role_column_count PLS_INTEGER;
    v_role_check_count  PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tables
    WHERE owner = 'APP_USER' AND table_name = 'TB_USER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE app_user.TB_USER (
                user_id         VARCHAR2(32 CHAR) DEFAULT RAWTOHEX(SYS_GUID()) NOT NULL,
                email           VARCHAR2(320 CHAR) NOT NULL,
                password_hash   VARCHAR2(255 CHAR) NOT NULL,
                display_name    NVARCHAR2(100),
                role            VARCHAR2(20 CHAR) DEFAULT ''user'' NOT NULL,
                is_active       NUMBER(1) DEFAULT 1 NOT NULL,
                created_at      TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                updated_at      TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                CONSTRAINT PK_USER PRIMARY KEY (user_id),
                CONSTRAINT UK_USER UNIQUE (email),
                CONSTRAINT CK_USER CHECK (is_active IN (0, 1)),
                CONSTRAINT CK_USER_ROLE CHECK (role IN (''admin'', ''user''))
            )';
    ELSE
        SELECT COUNT(*) INTO v_role_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_USER' AND table_name = 'TB_USER' AND column_name = 'ROLE';

        IF v_role_column_count = 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_user.TB_USER
                ADD (role VARCHAR2(20 CHAR) DEFAULT ''user'' NOT NULL)
            ';
        ELSE
            EXECUTE IMMEDIATE '
                UPDATE app_user.TB_USER
                SET role = ''user''
                WHERE role IS NULL OR role NOT IN (''admin'', ''user'')
            ';
            EXECUTE IMMEDIATE '
                ALTER TABLE app_user.TB_USER
                MODIFY (role DEFAULT ''user'' NOT NULL)
            ';
        END IF;

        SELECT COUNT(*) INTO v_role_check_count
        FROM all_constraints
        WHERE owner = 'APP_USER'
          AND table_name = 'TB_USER'
          AND constraint_name = 'CK_USER_ROLE';

        IF v_role_check_count = 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_user.TB_USER
                ADD CONSTRAINT CK_USER_ROLE CHECK (role IN (''admin'', ''user''))
            ';
        END IF;
    END IF;
END;
/

-- app_expense.TB_EXPENSE (002) declares a cross-schema FK to app_user.TB_USER.
GRANT REFERENCES, SELECT ON app_user.TB_USER TO app_expense;
