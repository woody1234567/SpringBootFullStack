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
-- Plain GRANT statements are used for GUI/JDBC clients that do not support
-- SQL*Plus slash-delimited anonymous blocks.
GRANT CREATE SESSION TO expense_tracker;

GRANT SELECT ON app_user.TB_USER TO expense_tracker;
GRANT EXECUTE ON app_user.SP_CREATE_USER TO expense_tracker;
GRANT EXECUTE ON app_user.SP_GET_USER_BY_EMAIL TO expense_tracker;

GRANT SELECT ON app_expense.TB_CATEGORY TO expense_tracker;
GRANT SELECT ON app_expense.TB_EXPENSE TO expense_tracker;
GRANT SELECT ON app_expense.TB_IMPORT_BATCH TO expense_tracker;
GRANT SELECT ON app_expense.TB_IMPORT_FAILED_ROW TO expense_tracker;
GRANT SELECT ON app_expense.TB_TMP_IMPORT_FAILED_ROW TO expense_tracker;

GRANT EXECUTE ON app_expense.TO_EXPENSE_IMPORT_ROW TO expense_tracker;
GRANT EXECUTE ON app_expense.TT_EXPENSE_IMPORT_ROW TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_CREATE_EXPENSE TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_UPDATE_EXPENSE TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_DELETE_EXPENSE TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_GET_EXPENSE_DETAIL TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_SEARCH_EXPENSE TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_GET_ACTIVE_CATEGORY TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_CREATE_IMPORT_BATCH TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_UPDATE_IMPORT_BATCH TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_CREATE_IMPORT_FAILED_ROW TO expense_tracker;
GRANT EXECUTE ON app_expense.SP_INSERT_IMPORTED_EXPENSE TO expense_tracker;
