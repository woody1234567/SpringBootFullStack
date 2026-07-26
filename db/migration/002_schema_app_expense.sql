-- Tables for expense tracking (categories, expenses, import batches).
-- The app_expense schema itself is created in 000_setup_schemas.sql.
DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tables
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_CATEGORY';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE app_expense.TB_CATEGORY (
                category_id     VARCHAR2(32 CHAR) DEFAULT RAWTOHEX(SYS_GUID()) NOT NULL,
                name            NVARCHAR2(100) NOT NULL,
                is_active       NUMBER(1) DEFAULT 1 NOT NULL,
                create_time     TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                update_time     TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                creator         VARCHAR2(32 CHAR),
                updater         VARCHAR2(32 CHAR),
                CONSTRAINT PK_CATEGORY PRIMARY KEY (category_id),
                CONSTRAINT UK_CATEGORY UNIQUE (name),
                CONSTRAINT CK_CATEGORY CHECK (is_active IN (0, 1))
            )';
    END IF;
END;
/

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_CATEGORY' AND column_name = 'CREATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_CATEGORY' AND column_name = 'CREATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_CATEGORY
                RENAME COLUMN created_at TO create_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_CATEGORY
                ADD (create_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_CATEGORY' AND column_name = 'UPDATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_CATEGORY' AND column_name = 'UPDATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_CATEGORY
                RENAME COLUMN updated_at TO update_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_CATEGORY
                ADD (update_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_CATEGORY' AND column_name = 'CREATOR';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_CATEGORY
            ADD (creator VARCHAR2(32 CHAR))
        ';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_CATEGORY' AND column_name = 'UPDATER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_CATEGORY
            ADD (updater VARCHAR2(32 CHAR))
        ';
    END IF;
END;
/

INSERT INTO app_expense.TB_CATEGORY (name)
SELECT t.name
FROM (
    SELECT '餐飲' AS name FROM dual UNION ALL
    SELECT '交通' FROM dual UNION ALL
    SELECT '住宿' FROM dual UNION ALL
    SELECT '娛樂' FROM dual UNION ALL
    SELECT '醫療' FROM dual UNION ALL
    SELECT '其他' FROM dual
) t
WHERE NOT EXISTS (SELECT 1 FROM app_expense.TB_CATEGORY);

COMMIT;

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tables
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_EXPENSE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE app_expense.TB_EXPENSE (
                expense_id      VARCHAR2(32 CHAR) DEFAULT RAWTOHEX(SYS_GUID()) NOT NULL,
                user_id         VARCHAR2(32 CHAR) NOT NULL,
                batch_id        VARCHAR2(32 CHAR),
                category_id     VARCHAR2(32 CHAR) NOT NULL,
                expense_date    DATE NOT NULL,
                amount          NUMBER(12,2) NOT NULL,
                invoice_number  VARCHAR2(20 CHAR),
                note            NVARCHAR2(500),
                create_time     TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                update_time     TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                creator         VARCHAR2(32 CHAR),
                updater         VARCHAR2(32 CHAR),
                CONSTRAINT PK_EXPENSE PRIMARY KEY (expense_id),
                CONSTRAINT FK_EXPENSE_1 FOREIGN KEY (user_id) REFERENCES app_user.TB_USER (user_id),
                CONSTRAINT FK_EXPENSE_2 FOREIGN KEY (category_id) REFERENCES app_expense.TB_CATEGORY (category_id),
                CONSTRAINT CK_EXPENSE CHECK (amount > 0)
            )';

        -- Index names must be schema-qualified: unlike SQL Server, Oracle otherwise
        -- creates the index in the CURRENT schema even when the table is elsewhere.
        EXECUTE IMMEDIATE '
            CREATE INDEX app_expense.IX_EXPENSE_1 ON app_expense.TB_EXPENSE (user_id, expense_date DESC)';
        EXECUTE IMMEDIATE '
            CREATE INDEX app_expense.IX_EXPENSE_2 ON app_expense.TB_EXPENSE (user_id, category_id)';

        -- Invoice numbers must be unique per user, but expenses WITHOUT an invoice
        -- number must not collide: Oracle (like SQL Server) treats a partially-null
        -- composite unique key as a duplicate when the non-null parts match, so a
        -- plain UNIQUE (user_id, invoice_number) would allow only ONE no-invoice
        -- expense per user. This function-based unique index produces an all-NULL
        -- key when invoice_number is NULL; all-NULL keys are not indexed, so
        -- uniqueness is enforced only when an invoice number is actually present
        -- (equivalent to the SQL Server filtered index that fixed this on master).
        EXECUTE IMMEDIATE '
            CREATE UNIQUE INDEX app_expense.UK_EXPENSE ON app_expense.TB_EXPENSE (
                CASE WHEN invoice_number IS NOT NULL THEN user_id END,
                CASE WHEN invoice_number IS NOT NULL THEN invoice_number END
            )';
    END IF;
END;
/

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_EXPENSE' AND column_name = 'CREATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_EXPENSE' AND column_name = 'CREATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_EXPENSE
                RENAME COLUMN created_at TO create_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_EXPENSE
                ADD (create_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_EXPENSE' AND column_name = 'UPDATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_EXPENSE' AND column_name = 'UPDATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_EXPENSE
                RENAME COLUMN updated_at TO update_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_EXPENSE
                ADD (update_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_EXPENSE' AND column_name = 'CREATOR';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_EXPENSE
            ADD (creator VARCHAR2(32 CHAR))
        ';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_EXPENSE' AND column_name = 'UPDATER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_EXPENSE
            ADD (updater VARCHAR2(32 CHAR))
        ';
    END IF;
END;
/

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tables
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_BATCH';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE app_expense.TB_IMPORT_BATCH (
                batch_id        VARCHAR2(32 CHAR) DEFAULT RAWTOHEX(SYS_GUID()) NOT NULL,
                user_id         VARCHAR2(32 CHAR) NOT NULL,
                file_name       NVARCHAR2(255),
                total_rows      NUMBER(10) NOT NULL,
                success_rows    NUMBER(10) NOT NULL,
                status          VARCHAR2(20 CHAR) NOT NULL,
                error_summary   NVARCHAR2(2000),
                create_time     TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                update_time     TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                creator         VARCHAR2(32 CHAR),
                updater         VARCHAR2(32 CHAR),
                CONSTRAINT PK_IMPORT_BATCH PRIMARY KEY (batch_id),
                CONSTRAINT FK_IMPORT_BATCH FOREIGN KEY (user_id) REFERENCES app_user.TB_USER (user_id)
            )';
    END IF;
END;
/

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_BATCH' AND column_name = 'CREATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_BATCH' AND column_name = 'CREATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_BATCH
                RENAME COLUMN created_at TO create_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_BATCH
                ADD (create_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_BATCH' AND column_name = 'UPDATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_BATCH' AND column_name = 'UPDATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_BATCH
                RENAME COLUMN updated_at TO update_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_BATCH
                ADD (update_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_BATCH' AND column_name = 'CREATOR';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_IMPORT_BATCH
            ADD (creator VARCHAR2(32 CHAR))
        ';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_BATCH' AND column_name = 'UPDATER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_IMPORT_BATCH
            ADD (updater VARCHAR2(32 CHAR))
        ';
    END IF;
END;
/

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE'
      AND table_name = 'TB_EXPENSE'
      AND column_name = 'BATCH_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_EXPENSE
            ADD batch_id VARCHAR2(32 CHAR)';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_constraints
    WHERE owner = 'APP_EXPENSE'
      AND table_name = 'TB_EXPENSE'
      AND constraint_name = 'FK_EXPENSE_3';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_EXPENSE
            ADD CONSTRAINT FK_EXPENSE_3 FOREIGN KEY (batch_id)
            REFERENCES app_expense.TB_IMPORT_BATCH (batch_id)';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_indexes
    WHERE owner = 'APP_EXPENSE'
      AND index_name = 'IX_EXPENSE_3';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE INDEX app_expense.IX_EXPENSE_3
            ON app_expense.TB_EXPENSE (batch_id)';
    END IF;
END;
/

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tables
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_FAILED_ROW';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE app_expense.TB_IMPORT_FAILED_ROW (
                failed_row_id  VARCHAR2(32 CHAR) DEFAULT RAWTOHEX(SYS_GUID()) NOT NULL,
                batch_id       VARCHAR2(32 CHAR) NOT NULL,
                row_number     NUMBER(10) NOT NULL,
                error_message  VARCHAR2(4000 CHAR) NOT NULL,
                create_time    TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                update_time    TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
                creator        VARCHAR2(32 CHAR),
                updater        VARCHAR2(32 CHAR),
                CONSTRAINT PK_IMPORT_FAILED_ROW PRIMARY KEY (failed_row_id),
                CONSTRAINT FK_IMPORT_FAILED_ROW_1 FOREIGN KEY (batch_id)
                    REFERENCES app_expense.TB_IMPORT_BATCH (batch_id)
            )';

        EXECUTE IMMEDIATE '
            CREATE INDEX app_expense.IX_IMPORT_FAILED_ROW_1
            ON app_expense.TB_IMPORT_FAILED_ROW (batch_id, row_number)';
    END IF;
END;
/

DECLARE
    v_count PLS_INTEGER;
    v_old_column_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_FAILED_ROW' AND column_name = 'CREATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_FAILED_ROW' AND column_name = 'CREATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_FAILED_ROW
                RENAME COLUMN created_at TO create_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_FAILED_ROW
                ADD (create_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_FAILED_ROW' AND column_name = 'UPDATE_TIME';

    IF v_count = 0 THEN
        SELECT COUNT(*) INTO v_old_column_count
        FROM all_tab_columns
        WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_FAILED_ROW' AND column_name = 'UPDATED_AT';

        IF v_old_column_count > 0 THEN
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_FAILED_ROW
                RENAME COLUMN updated_at TO update_time
            ';
        ELSE
            EXECUTE IMMEDIATE '
                ALTER TABLE app_expense.TB_IMPORT_FAILED_ROW
                ADD (update_time TIMESTAMP(3) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL)
            ';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_FAILED_ROW' AND column_name = 'CREATOR';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_IMPORT_FAILED_ROW
            ADD (creator VARCHAR2(32 CHAR))
        ';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM all_tab_columns
    WHERE owner = 'APP_EXPENSE' AND table_name = 'TB_IMPORT_FAILED_ROW' AND column_name = 'UPDATER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE app_expense.TB_IMPORT_FAILED_ROW
            ADD (updater VARCHAR2(32 CHAR))
        ';
    END IF;
END;
/
