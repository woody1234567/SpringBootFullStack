---
name: oracle-database-business-logic
description: Oracle-centric database business logic for this project. Use this skill whenever creating or modifying Oracle DDL, PL/SQL packages, stored procedures, functions, table functions, result-code conventions, exception handling, transaction ownership, or Spring Repository integration that calls Oracle database objects. Also use oracle-naming-convention whenever any Oracle object is created, renamed, or reviewed.
---

# Oracle Database Business Logic

## Core Principle

This project is database-centric. Business rules, calculations, validations, data consistency rules, workflow state transitions, permission-related database checks, batch logic, business identifier generation, aggregation, transformation, and transactional data operations should primarily live in Oracle Database.

The Java backend should not duplicate business rules already implemented in Oracle. Only repeat validation in Java when it is needed for HTTP request validation, security protection, or early input rejection.

## Oracle Organization

Prefer packages over loose standalone procedures:

```text
Package: PG_USER
├── Procedure: SP_CREATE_USER
├── Procedure: SP_UPDATE_USER
├── Procedure: SP_GET_USER_DETAIL
├── Procedure: SP_ASSIGN_USER_ROLE
└── Function:  FN_USER_EXISTS
```

Prefer stored procedures over functions for application workflows. Use stored procedures for commands, multi-step workflows, output parameters, transaction-controlled operations, batch writes, result-code conventions, and reusable read workflows that return result sets through `SYS_REFCURSOR`.

Use functions only when the database object naturally behaves like a function: scalar existence checks, reusable calculations, deterministic value derivation, or a true table function needed as a composable SQL row source. Do not use functions to carry command workflows, writes, business result codes, or multi-output API contracts.

Do not create or use Oracle views in this project. Keep reusable read logic behind stored procedures with explicit result codes and cursor outputs, so the Java Repository layer uses one consistent Oracle integration style. Use table functions only when callers truly need composable SQL row sources; do not use them as a view substitute for ordinary API reads. Use scalar functions carefully in large queries because row-by-row SQL-to-PL/SQL context switching can hurt performance.

When creating or modifying any Oracle object, follow the `oracle-naming-convention` skill exactly.

## Batch Table Types

For batch write workflows, prefer SQL-level object types and nested table types to carry structured input and output rows between Spring and Oracle. Do not encode row lists as comma-delimited strings, ad hoc JSON, or parallel scalar arrays unless a specific integration constraint requires it.

Create one object type for a row and one table type for a collection of those rows:

```sql
CREATE TYPE TO_EXPENSE_IMPORT_ROW AS OBJECT (
    EXPENSE_DATE VARCHAR2(10 CHAR),
    CATEGORY_ID VARCHAR2(32 CHAR),
    AMOUNT NUMBER,
    NOTE NVARCHAR2(2000)
);

CREATE TYPE TT_EXPENSE_IMPORT_ROW AS TABLE OF TO_EXPENSE_IMPORT_ROW;

CREATE TYPE TO_EXPENSE_IMPORT_RESULT_ROW AS OBJECT (
    ROW_NUMBER NUMBER,
    RESULT_CODE VARCHAR2(50 CHAR),
    RESULT_MESSAGE VARCHAR2(4000 CHAR)
);

CREATE TYPE TT_EXPENSE_IMPORT_RESULT_ROW AS TABLE OF TO_EXPENSE_IMPORT_RESULT_ROW;
```

Use table type parameters when the procedure accepts or returns row-shaped batch data:

```sql
PROCEDURE SP_IMPORT_EXPENSE_BATCH (
    I_USER_ID IN VARCHAR2,
    I_ROWS IN TT_EXPENSE_IMPORT_ROW,
    O_FAILED_ROWS OUT TT_EXPENSE_IMPORT_RESULT_ROW,
    O_RESULT_CODE OUT VARCHAR2,
    O_RESULT_MESSAGE OUT VARCHAR2
);
```

Use `I_...` for input table parameters and `O_...` for output table parameters. Keep type attributes aligned with the table columns or API row contract, and update the Oracle type, PL/SQL procedure signature, Java `SimpleJdbcCall` declaration, row binding code, DTOs, and API documentation together when the row shape changes.

Use SQL object types created with `CREATE TYPE`, not PL/SQL-only package record types, when Spring JDBC must bind or read the collection. Spring JDBC should bind these table parameters with Oracle ARRAY support and declare them with the exact database type name.

## Spring JDBC Integration

Use `SimpleJdbcCall` for Oracle stored procedures when it fits the existing project style.

For a procedure inside a package, use `withCatalogName` for the Oracle package:

```java
SimpleJdbcCall createUserCall = new SimpleJdbcCall(jdbcTemplate)
        .withSchemaName("APP_USER")
        .withCatalogName("PG_USER")
        .withProcedureName("SP_CREATE_USER");
```

When metadata lookup is unreliable or costly, explicitly declare parameters:

```java
SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
        .withSchemaName("APP_USER")
        .withCatalogName("PG_USER")
        .withProcedureName("SP_CREATE_USER")
        .withoutProcedureColumnMetaDataAccess()
        .declareParameters(
                new SqlParameter("I_USERNAME", Types.VARCHAR),
                new SqlParameter("I_DISPLAY_NAME", Types.NVARCHAR),
                new SqlOutParameter("O_USER_ID", Types.NUMERIC),
                new SqlOutParameter("O_RESULT_CODE", Types.VARCHAR),
                new SqlOutParameter("O_RESULT_MESSAGE", Types.VARCHAR)
        );
```

PL/SQL parameter names have no `@` prefix. This project uses `I_...` for input parameters and `O_...` for output parameters. Spring JDBC map keys must match declared PL/SQL parameter names exactly.

Use named constants for reused parameter names:

```java
private static final String I_USERNAME = "I_USERNAME";
private static final String O_RESULT_CODE = "O_RESULT_CODE";
```

Do not scatter raw Oracle parameter names across multiple classes.

## Query Results and Functions

Oracle returns ordinary result sets through a `SYS_REFCURSOR` output parameter:

```java
SimpleJdbcCall getUserCall = new SimpleJdbcCall(jdbcTemplate)
        .withSchemaName("APP_USER")
        .withCatalogName("PG_USER")
        .withProcedureName("SP_GET_USER_DETAIL")
        .withoutProcedureColumnMetaDataAccess()
        .declareParameters(
                new SqlParameter("I_USER_ID", Types.NUMERIC),
                new SqlOutParameter("O_USERS", OracleTypes.CURSOR, new UserRowMapper())
        );
```

Prefer explicit `SYS_REFCURSOR` OUT parameters over implicit result sets such as `DBMS_SQL.RETURN_RESULT`, unless the project already documents implicit result sets as its pattern.

Call scalar functions with `JdbcTemplate` or `NamedParameterJdbcTemplate` using `SELECT ... FROM dual`:

```java
Boolean exists = jdbcTemplate.queryForObject(
        "SELECT PG_USER.FN_USER_EXISTS(?) FROM dual",
        Boolean.class,
        username
);
```

A function called from SQL cannot return a PL/SQL `BOOLEAN`. Boolean-style functions must return `NUMBER(1)` with `0` and `1`.

Call table functions with `SELECT * FROM TABLE(...)`:

```java
List<UserRow> users = namedParameterJdbcTemplate.query(
        "SELECT * FROM TABLE(PG_USER.FN_SEARCH_USERS(:I_KEYWORD))",
        Map.of("I_KEYWORD", keyword),
        userRowMapper
);
```

## Result Convention

Stored procedures should return predictable result information:

```text
O_RESULT_CODE       OUT VARCHAR2(50)
O_RESULT_MESSAGE    OUT VARCHAR2(4000)
```

For object creation operations, also return:

```text
O_CREATED_ID        OUT NUMBER
```

Recommended semantic result codes:

```text
SUCCESS
VALIDATION_ERROR
NOT_FOUND
DUPLICATE
FORBIDDEN
SYSTEM_ERROR
```

Use a documented numeric code convention only if the existing project already uses one. Do not mix unrelated result-code styles.

Do not use a function return value to carry business result codes when output parameters are the documented convention. Keep result codes, output parameters, and cursor result sets clearly separated.

The Repository or Service layer maps Oracle result codes to Java exceptions and HTTP responses.

## Exception Handling

Stored procedures should handle expected business conditions explicitly and use `EXCEPTION` blocks for unexpected database errors.

For duplicate key violations, handle `DUP_VAL_ON_INDEX` when a specific duplicate response is required. Prefer enforcing uniqueness with constraints or indexes rather than relying only on preliminary existence checks.

Use these inside `EXCEPTION` blocks when detailed server-side logging is required:

```text
SQLCODE
SQLERRM
DBMS_UTILITY.FORMAT_ERROR_STACK
DBMS_UTILITY.FORMAT_ERROR_BACKTRACE
```

Do not expose sensitive database details, schema names, SQL statements, stack traces, password hashes, or internal implementation details through API responses. Log detailed errors on the backend or in an approved audit mechanism, but return safe user-facing messages.

## Transaction Ownership

Business transactions are always Spring-owned in this project.

Use Spring `@Transactional` boundaries in the Service layer to control commit and rollback behavior. Oracle stored procedures must not issue `COMMIT` or `ROLLBACK`; let the JDBC connection and Spring transaction manager control the transaction.

Do not use database-owned transactions for application workflows. Stored procedures should report success or failure through output parameters and exceptions, then let the Repository or Service layer decide how the outer Spring transaction is completed.

Avoid `PRAGMA AUTONOMOUS_TRANSACTION` in application business logic because it creates independent database transaction control that bypasses the Spring-owned transaction boundary.
