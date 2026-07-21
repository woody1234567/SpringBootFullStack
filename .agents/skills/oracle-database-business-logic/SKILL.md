---
name: oracle-database-business-logic
description: Oracle-centric database business logic for this project. Use this skill whenever creating or modifying Oracle DDL, PL/SQL packages, stored procedures, functions, table functions, result-code conventions, exception handling, transaction ownership, or Spring Repository integration that calls Oracle database objects. Also use oracle-naming-convention whenever any Oracle object is created, renamed, or reviewed.
---

# Oracle Database Business Logic

## Core Principle

This project is database-centric. Business rules, calculations, validations, data consistency rules, workflow state transitions, permission-related database checks, batch logic, business identifier generation, aggregation, transformation, and transactional data operations should primarily live in Oracle Database.

The Java backend should not duplicate business rules already implemented in Oracle. Only repeat validation in Java when it is needed for HTTP request validation, security protection, or early input rejection.

## Oracle Organization

Use schemas as module boundaries, such as `app_user`, `app_order`, or `security`.

Prefer packages over loose standalone procedures:

```text
Schema: app_user
└── Package: PG_USER
    ├── Procedure: SP_CREATE_USER
    ├── Procedure: SP_UPDATE_USER
    ├── Procedure: SP_VERIFY_EMAIL
    ├── Procedure: SP_RESET_PASSWORD
    ├── Procedure: SP_GET_USER_DETAIL
    └── Function:  FN_EMAIL_EXISTS
```

Prefer schema-qualified object names:

```sql
app_user.PG_USER.SP_CREATE_USER
app_order.PG_ORDER.SP_CREATE_ORDER
security.PG_SECURITY.FN_CHECK_PERMISSION
```

Use stored procedures for commands, multi-step workflows, output parameters, transaction-controlled operations, and reusable read workflows that return result sets through `SYS_REFCURSOR`.

Do not create or use Oracle views in this project. Keep reusable read logic behind stored procedures with explicit result codes and cursor outputs, so the Java Repository layer uses one consistent Oracle integration style. Use table functions only when callers truly need composable SQL row sources; do not use them as a view substitute for ordinary API reads. Use scalar functions carefully in large queries because row-by-row SQL-to-PL/SQL context switching can hurt performance.

When creating or modifying any Oracle object, follow the `oracle-naming-convention` skill exactly.

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
                new SqlParameter("p_email", Types.VARCHAR),
                new SqlParameter("p_password_hash", Types.VARCHAR),
                new SqlOutParameter("p_user_id", Types.NUMERIC),
                new SqlOutParameter("p_result_code", Types.VARCHAR),
                new SqlOutParameter("p_result_message", Types.VARCHAR)
        );
```

PL/SQL parameter names have no `@` prefix. This project uses the `p_` prefix for procedure and function parameters. Spring JDBC map keys must match declared PL/SQL parameter names exactly.

Use named constants for reused parameter names:

```java
private static final String P_EMAIL = "p_email";
private static final String P_RESULT_CODE = "p_result_code";
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
                new SqlParameter("p_user_id", Types.NUMERIC),
                new SqlOutParameter("p_users", OracleTypes.CURSOR, new UserRowMapper())
        );
```

Prefer explicit `SYS_REFCURSOR` OUT parameters over implicit result sets such as `DBMS_SQL.RETURN_RESULT`, unless the project already documents implicit result sets as its pattern.

Call scalar functions with `JdbcTemplate` or `NamedParameterJdbcTemplate` using `SELECT ... FROM dual`:

```java
Boolean exists = jdbcTemplate.queryForObject(
        "SELECT app_user.PG_USER.FN_EMAIL_EXISTS(?) FROM dual",
        Boolean.class,
        email
);
```

A function called from SQL cannot return a PL/SQL `BOOLEAN`. Boolean-style functions must return `NUMBER(1)` with `0` and `1`.

Call table functions with `SELECT * FROM TABLE(...)`:

```java
List<UserRow> users = namedParameterJdbcTemplate.query(
        "SELECT * FROM TABLE(app_user.PG_USER.FN_SEARCH_USERS(:p_keyword))",
        Map.of("p_keyword", keyword),
        userRowMapper
);
```

## Result Convention

Stored procedures should return predictable result information:

```text
p_result_code       OUT VARCHAR2(50)
p_result_message    OUT VARCHAR2(4000)
```

For object creation operations, also return:

```text
p_created_id        OUT NUMBER
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

Business transactions need a clear owner.

Use database-owned transactions when one stored procedure performs the full workflow and intentionally owns commit or rollback. In that mode, the procedure may `COMMIT` after the workflow succeeds and `ROLLBACK` in unexpected failure handling.

Use Spring-owned transactions when one Service method coordinates multiple Repository operations, or when transaction ownership belongs to the application layer. In this mode, called stored procedures must not issue `COMMIT` or `ROLLBACK`; let the JDBC connection and Spring transaction manager control the outer transaction.

Oracle has no nested transactions. A procedure that may run inside an existing transaction must not blindly roll back work it does not own. When nested-safe behavior is needed, use a `SAVEPOINT`, roll back to that savepoint on failure, and document the strategy.

Use `PRAGMA AUTONOMOUS_TRANSACTION` sparingly, only for genuinely independent writes such as audit logging that must survive rollback of the main transaction. Autonomous blocks must issue their own `COMMIT` or `ROLLBACK` and must be documented.
