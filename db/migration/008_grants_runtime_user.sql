-- Grants required by the Spring Boot runtime account.
--
-- Run as a DBA/admin account after scripts 000-007. The runtime account is kept
-- separate from the object-owning schemas: APP_USER and APP_EXPENSE own tables,
-- types, and procedures; EXPENSE_TRACKER only connects and executes the
-- approved API surface.
--
-- If the user does not exist yet, create it first:
--   CREATE USER expense_tracker IDENTIFIED BY "<strong-password>";
--
-- These SELECT grants also make the tables visible/queryable from tools such as
-- VSCode Database Client when connected as EXPENSE_TRACKER.
DECLARE
    v_runtime_user CONSTANT VARCHAR2(128) := 'EXPENSE_TRACKER';
    v_count        PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM dba_users
    WHERE username = v_runtime_user;

    IF v_count = 0 THEN
        RAISE_APPLICATION_ERROR(
            -20000,
            'Runtime user EXPENSE_TRACKER does not exist. Create it before running grants.'
        );
    END IF;

    EXECUTE IMMEDIATE 'GRANT CREATE SESSION TO ' || v_runtime_user;

    EXECUTE IMMEDIATE 'GRANT SELECT ON app_user.TB_USER TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_user.SP_CREATE_USER TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_user.SP_GET_USER_BY_EMAIL TO ' || v_runtime_user;

    EXECUTE IMMEDIATE 'GRANT SELECT ON app_expense.TB_CATEGORY TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT SELECT ON app_expense.TB_EXPENSE TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT SELECT ON app_expense.TB_IMPORT_BATCH TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT SELECT ON app_expense.TB_TMP_IMPORT_FAILED_ROW TO ' || v_runtime_user;

    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.TO_EXPENSE_IMPORT_ROW TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.TT_EXPENSE_IMPORT_ROW TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_CREATE_EXPENSE TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_UPDATE_EXPENSE TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_DELETE_EXPENSE TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_GET_EXPENSE_DETAIL TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_SEARCH_EXPENSE TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_GET_ACTIVE_CATEGORY TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_CREATE_IMPORT_BATCH TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_UPDATE_IMPORT_BATCH TO ' || v_runtime_user;
    EXECUTE IMMEDIATE 'GRANT EXECUTE ON app_expense.SP_IMPORT_EXPENSE_BATCH TO ' || v_runtime_user;
END;
/
