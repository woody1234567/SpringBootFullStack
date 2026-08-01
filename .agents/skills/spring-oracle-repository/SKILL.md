---
name: spring-oracle-repository
description: Spring Boot Repository conventions for Oracle integration in this project. Use this skill whenever adding or modifying Repository interfaces or implementations, JdbcTemplate, NamedParameterJdbcTemplate, SimpleJdbcCall, Oracle package/procedure/function calls, SYS_REFCURSOR mapping, SQL object/table type binding, repository model classes, or Oracle parameter declarations.
---

# Spring Oracle Repository

## Responsibility

Repositories are the Oracle integration boundary. They call Oracle procedures/functions, execute SQL queries when appropriate, register Oracle parameters including `SYS_REFCURSOR`, bind SQL object/table types, map result sets, convert database outputs into repository result model classes, and translate low-level database exceptions when appropriate.

Keep business decisions in Oracle or Services. Keep HTTP response behavior and authorization outside Repositories.

## Repository Rules

Repositories should:

- Provide an interface plus an implementation under `repository/impl` when matching the existing style.
- Use `SimpleJdbcCall` for stored procedures and packaged procedures when it fits the project pattern.
- Use `JdbcTemplate` or `NamedParameterJdbcTemplate` for focused SQL calls, scalar function calls, and simple lookups.
- Declare Oracle parameters explicitly when metadata lookup is unreliable or costly.
- Keep parameter names aligned with PL/SQL names, including `I_...` input parameters and `O_...` output parameters.
- Use named constants for reused Oracle parameter names and result keys.
- Map `SYS_REFCURSOR` rows through dedicated row mappers under `repository/mapper`.
- Return typed repository model classes from `repository/model`, not raw JDBC or Oracle driver objects.

Repositories must not contain business rules, HTTP status mapping, Spring Security authorization, Service workflow orchestration, or transaction ownership decisions.

Use `oracle-database-business-logic` and `oracle-naming-convention` whenever changing Oracle packages, procedures, functions, object types, table types, result-code conventions, or PL/SQL parameter names.

## Method Naming

Method names should reflect database operations clearly:

```text
createExpense
updateExpense
getExpenseDetail
searchExpenses
categoryExists
importExpenseBatch
```

Avoid generic names like `execute`, `run`, `call`, or `process` except for internal reusable helpers.

## SimpleJdbcCall

For packaged procedures, use `withCatalogName` for the Oracle package:

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
                new SqlOutParameter("O_USER_ID", Types.VARCHAR),
                new SqlOutParameter("O_RESULT_CODE", Types.VARCHAR),
                new SqlOutParameter("O_RESULT_MESSAGE", Types.VARCHAR)
        );
```

Spring JDBC map keys must match declared PL/SQL parameter names exactly.

Use named constants for reused parameter names:

```java
private static final String I_USERNAME = "I_USERNAME";
private static final String O_RESULT_CODE = "O_RESULT_CODE";
```

Do not scatter raw Oracle parameter names across multiple classes.

## Cursors and Row Mappers

Declare cursor outputs explicitly:

```java
new SqlOutParameter("O_USERS", OracleTypes.CURSOR, new UserRowMapper())
```

Place reusable row mappers under `repository/mapper`. Keep row mappers focused on converting database columns into repository row/model classes. Do not put response DTO mapping or business exception mapping in row mappers.

## Repository Models

Create repository result models as Java classes under `repository/model` and use Lombok for getters, constructors, and builders when useful.

Do not pass raw `Map<String, Object>`, `SqlParameterSource`, JDBC `ResultSet`, Oracle `STRUCT`, Oracle `ARRAY`, or cursor objects across the Repository boundary. Repositories should convert database outputs into typed repository model classes before returning to Services.

## Batch Oracle Types

For batch write workflows, prefer SQL-level Oracle object types and table types defined by the database layer. Bind structured collections through Oracle ARRAY support with the exact database type names.

Do not encode row lists as comma-delimited strings, ad hoc JSON, or parallel scalar arrays unless a specific integration constraint requires it. Keep Oracle type attributes, PL/SQL procedure signatures, Java binding code, repository models, DTOs, and API documentation aligned when the row shape changes.

## Exception Handling

Translate low-level database exceptions only when doing so adds useful repository context. Do not expose raw Oracle exceptions, `ORA-` messages, SQL text, schema details, or connection details beyond the Repository boundary.
