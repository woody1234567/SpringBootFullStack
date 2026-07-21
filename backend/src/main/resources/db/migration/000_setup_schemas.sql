-- Creates the two application schemas (Oracle schema = user).
--
-- Run this script as a DBA account (e.g. SYSTEM), connected to the application
-- PDB. The schemas are created as schema-only accounts (NO AUTHENTICATION), so
-- they own objects but cannot log in.
--
-- Scripts 001-007 are then run by an admin account holding CREATE ANY TABLE,
-- CREATE ANY INDEX, CREATE ANY PROCEDURE and CREATE ANY TYPE
-- (objects are created with schema-qualified names; the definer of each object
-- is the owning schema). This mirrors how the SQL Server scripts were run as dbo.
DECLARE
    v_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM dba_users WHERE username = 'APP_USER';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER app_user NO AUTHENTICATION
                           DEFAULT TABLESPACE users QUOTA UNLIMITED ON users';
    END IF;

    SELECT COUNT(*) INTO v_count FROM dba_users WHERE username = 'APP_EXPENSE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER app_expense NO AUTHENTICATION
                           DEFAULT TABLESPACE users QUOTA UNLIMITED ON users';
    END IF;
END;
/
