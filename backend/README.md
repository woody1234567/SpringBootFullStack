# Expense Tracker – Backend

Spring Boot 3 (Java 21) REST API for a personal expense/invoice tracker. It follows a
database-centric architecture: business rules, validation, and multi-table workflows live in
Oracle stored procedures and functions; the Java layers handle HTTP, security, and
orchestration only.

```text
Vue 3 + TypeScript Frontend
        ↓
Spring Boot REST API (Controller → Service → Repository)
        ↓
Oracle Stored Procedures / Functions / Views
        ↓
Oracle Tables
```

## Core Features

- **Authentication** (`/api/auth`) – Email/password registration and login. Passwords are hashed
  with BCrypt; `app_user.SP_CREATE_USER` and the login lookup enforce email uniqueness and account
  status and role in the database. A successful login/register returns a JWT (`AuthResponse`) with
  the user's role, used as a stateless Bearer token for all subsequent requests
  (`SessionCreationPolicy.STATELESS`, `JwtAuthenticationFilter`).
- **Expense CRUD** (`/api/expenses`) – Create, update, delete, fetch-by-id, and paginated
  search (filterable by date range and category) for the authenticated user's own expenses.
  Each operation maps to a stored procedure (`app_expense.SP_CREATE_EXPENSE`,
  `SP_UPDATE_EXPENSE`, etc.) that owns validation (positive amount, valid category, duplicate
  invoice number per user) and returns a `p_result_code`/`p_result_message` pair that the repository
  translates into typed results and, ultimately, HTTP status codes.
- **Categories** (`/api/categories`) – Read-only list of active expense categories, backed by
  the `app_expense.SP_GET_ACTIVE_CATEGORY` stored procedure.
- **CSV batch import** (`/api/imports/expenses`) – Multipart upload of a CSV file
  (`expense_date, amount, category, invoice_number, note` columns) to bulk-create expenses.
  `CsvExpenseParser` handles structural parsing only (headers, type coercion, BOM/UTF-8); all
  business validation happens in `app_expense.SP_IMPORT_EXPENSE_BATCH`, which is **all-or-nothing**
  — if any row fails, the whole batch is rolled back and the failing rows are returned with
  reasons, while an audit row is still written to `import_batches` either way.
- **Standardized API responses** – Every endpoint returns the same envelope
  (`{ "success", "message", "data" }` on success; `{ "success": false, "message", "errorCode",
  "errors" }` on failure) via a `@RestControllerAdvice` global exception handler that maps
  business exceptions to HTTP status codes without leaking Oracle internals.
- **Centralized AOP logging** – A single Spring AOP aspect (`LoggingAspect`) logs method entry,
  exit, duration, and exceptions across the Controller, Service, and Repository layers, so no
  individual class hand-writes entry/exit logging anymore. See [Logging](#logging) below.
- **API documentation** – springdoc-openapi is wired up; Swagger UI and the OpenAPI JSON are
  exposed only under the `dev`/`local` Spring profiles (see [Security](#security) below).

## Architecture / Package Layout

```text
src/main/java/com/example/expensetracker
├── aspect            # LoggingAspect (cross-cutting entry/exit/exception logging) + @LoggedOperation
├── config          # Security, CORS, OpenAPI wiring
├── constant        # Result-code and SQL parameter-name constants
├── controller       # HTTP endpoints only — no SQL, no business logic
├── dto
│   ├── request     # Validated inbound DTOs
│   └── response    # Outbound DTOs (ApiResponse envelope, PageResponse, etc.)
├── exception        # Business exceptions + global exception handler
├── repository        # JdbcTemplate/SimpleJdbcCall calls into stored procedures
│   ├── model        # Raw repository result shapes (not exposed to clients)
│   ├── mapper        # RowMapper implementations
│   └── impl
├── security          # JWT provider/filter, authenticated-principal resolution
├── service           # Orchestration, security context, DTO translation
└── util              # CSV parsing helper, SensitiveDataMasker (log redaction)

src/main/resources
├── application.yml           # Local config (gitignored — copy from the example below)
├── application-example.yml   # Template; copy to application.yml and fill in real values
├── log4j2.xml
└── db/migration/             # Numbered Oracle DDL/stored-procedure scripts (see below)
```

## Database Objects

Oracle schemas (users) group related objects, applied in order from `src/main/resources/db/migration/`:

| File | Purpose |
| --- | --- |
| `000_setup_schemas.sql` | Creates the `app_user` and `app_expense` schema-only accounts (run as DBA) |
| `001_schema_app_user.sql` | `app_user.TB_USER` table, including the `role` column and role check constraint |
| `002_schema_app_expense.sql` | `TB_CATEGORY`, `TB_EXPENSE`, `TB_IMPORT_BATCH` tables (incl. function-based unique index so multiple no-invoice expenses are allowed per user) |
| `003_types_app_expense.sql` | `TO_EXPENSE_IMPORT_ROW`/`TT_EXPENSE_IMPORT_ROW` collection types and `TB_TMP_IMPORT_FAILED_ROW` GTT for batch import |
| `004_procs_app_user.sql` | `app_user.SP_CREATE_USER`, `SP_GET_USER_BY_EMAIL` |
| `005_procs_app_expense_crud.sql` | `app_expense.SP_CREATE_EXPENSE`, `SP_UPDATE_EXPENSE`, `SP_DELETE_EXPENSE`, `SP_GET_EXPENSE_DETAIL`, `SP_SEARCH_EXPENSE` |
| `006_proc_active_categories.sql` | `app_expense.SP_GET_ACTIVE_CATEGORY` |
| `007_proc_import_expenses_batch.sql` | `app_expense.SP_IMPORT_EXPENSE_BATCH` — atomic CSV bulk import |
| `008_grants_runtime_user.sql` | Grants `EXPENSE_TRACKER` the execute/select privileges needed by the backend and local database tools |
| `009_recompile_search_expense.sql` | Recompiles `app_expense.SP_SEARCH_EXPENSE` with stable `ROW_NUMBER()` pagination |

These scripts are **not** run automatically (no Flyway/Liquibase dependency is wired in) — apply
them manually and in numeric order against your Oracle database before starting the app,
e.g. with SQL*Plus or SQLcl.

Primary and foreign-key ID columns use `VARCHAR2(32 CHAR)` values generated by
`RAWTOHEX(SYS_GUID())`, so API payloads and JWT subjects treat IDs as strings rather than numbers.

### User Roles

User roles are stored on `app_user.TB_USER.role` as lowercase values:

| Role | Spring authority | Current access |
| --- | --- | --- |
| `admin` | `ROLE_ADMIN` | All existing protected APIs |
| `user` | `ROLE_USER` | All existing protected APIs |

`001_schema_app_user.sql` creates `role VARCHAR2(20 CHAR) DEFAULT 'user' NOT NULL` and enforces
`CK_USER_ROLE CHECK (role IN ('admin', 'user'))`. The same script also handles an existing
`TB_USER` table by adding the column if it is missing, defaulting/backfilling rows to `user`, and
adding the check constraint.

Public registration does not accept a role and always creates ordinary `user` accounts through the
database default. Admin access is assigned deliberately at the database layer, for example:

```sql
UPDATE app_user.TB_USER
SET role = 'admin'
WHERE email = 'admin@example.com';
```

`app_user.SP_GET_USER_BY_EMAIL` returns the role during login so the backend can include it in both
`AuthResponse.user.role` and the issued JWT.

Before running `008_grants_runtime_user.sql`, create the runtime connection user with a real
password:

```sql
CREATE USER expense_tracker IDENTIFIED BY "<strong-password>";
```

The runtime user is intentionally separate from the object-owning schemas. `APP_USER` and
`APP_EXPENSE` own the tables/procedures/types; `EXPENSE_TRACKER` connects from Spring Boot and
receives only the required privileges. When connected as `EXPENSE_TRACKER` in VSCode Database
Client, use the schema-qualified names (`app_user.TB_USER`, `app_expense.TB_EXPENSE`) or expand
the `APP_USER` / `APP_EXPENSE` schemas under the client schema browser.

Stored procedures follow a consistent result convention: business outcomes are reported via
`p_result_code` / `p_result_message` output parameters (`SUCCESS`, `VALIDATION_ERROR`, `NOT_FOUND`,
`DUPLICATE`, `SYSTEM_ERROR`, ...), which the repository layer maps to Java exceptions and the
global exception handler maps to HTTP status codes.

## Updating Tables

Database table changes are managed through the numbered SQL scripts in
`src/main/resources/db/migration/`. Because this project does not wire in Flyway or Liquibase,
apply each script manually with SQL*Plus, SQLcl, VSCode Database Client, or another Oracle client.

For a clean local database that can be rebuilt, update the existing schema scripts and re-apply the
full migration chain in order:

```text
000_setup_schemas.sql
001_schema_app_user.sql
002_schema_app_expense.sql
003_types_app_expense.sql
004_procs_app_user.sql
005_procs_app_expense_crud.sql
006_proc_active_categories.sql
007_proc_import_expenses_batch.sql
008_grants_runtime_user.sql
009_recompile_search_expense.sql
```

For a database that already contains data or has been shared with others, do not rewrite a migration
that has already been applied. Add the next numbered migration instead, for example
`010_alter_expense_tables.sql`, and include the required `ALTER TABLE`, data backfill, index,
constraint, procedure, and grant changes there.

Table-change checklist:

- Create tables under the owning schema (`app_user` or `app_expense`), not under the runtime user.
- Use schema-qualified object names such as `app_expense.TB_EXPENSE`.
- Follow Oracle naming rules: `TB_` tables, `PK_` primary keys, `FK_` foreign keys, `UK_` unique
  keys, `IX_` indexes, and `SP_` stored procedures.
- Store ID columns as `VARCHAR2(32 CHAR)` and generate new IDs with `RAWTOHEX(SYS_GUID())`.
- Keep foreign-key columns the same type and length as the referenced ID column.
- Put reusable reads behind stored procedures returning `SYS_REFCURSOR`; do not create or use
  Oracle views for API reads in this project.
- Update related stored procedures whenever a table column, validation rule, or returned result set
  changes.
- Update Java repository models, row mappers, DTOs, frontend TypeScript types, and API forms when
  the database shape changes.
- After creating or changing tables, procedures, or types, verify that the `EXPENSE_TRACKER`
  runtime user has the needed `SELECT` or `EXECUTE` grants.
- If Oracle reports `PLS-00905: object ... is invalid`, inspect the compile errors before changing
  Java code:
  ```sql
  SELECT name, type, line, position, text
  FROM all_errors
  WHERE owner = 'APP_EXPENSE'
    AND name = 'SP_SEARCH_EXPENSE'
  ORDER BY sequence;
  ```
  Then re-run the migration that owns the procedure, or apply the next repair migration when the
  database already contains data.
- Re-run backend tests after code changes:
  ```sh
  mvn test
  ```

Minimal examples:

```sql
ALTER TABLE app_expense.TB_EXPENSE
ADD merchant_name VARCHAR2(100 CHAR);

CREATE INDEX app_expense.IX_EXPENSE_3
ON app_expense.TB_EXPENSE (expense_date);

GRANT SELECT ON app_expense.TB_EXPENSE TO expense_tracker;
```

## Security

- Stateless JWT authentication (`io.jsonwebtoken` / jjwt) — no server-side session state.
- `/api/auth/**` is public; every other endpoint requires a valid Bearer token whose role is
  `admin` or `user`.
- A successful login/register returns `AuthResponse` as `{ token, user }`; `user` contains
  `userId`, `email`, `displayName`, and `role`.
- JWTs use the GUID `userId` as the `subject` and include custom `email` and `role` claims.
  `JwtTokenProvider.parseToken(...)` rejects tokens with missing, malformed, or unsupported roles.
- `JwtAuthenticationFilter` maps the JWT role claim to Spring Security authorities:
  `admin` -> `ROLE_ADMIN`, `user` -> `ROLE_USER`.
- `SecurityConfig` currently allows both roles on all protected backend features via
  `hasAnyRole("ADMIN", "USER")`; no existing feature is admin-only yet.
- Tokens issued before role support do not contain the `role` claim and must be replaced by logging
  in again.
- Swagger UI (`/swagger-ui.html`) and the OpenAPI doc (`/api-docs`) are only exposed when the
  active Spring profile is `dev` or `local` — they are locked down (require auth) otherwise.
- CORS allowed origins are configurable via `app.cors.allowed-origins` (comma-separated).
- Passwords are hashed with `BCryptPasswordEncoder`; plaintext passwords, password hashes, and
  full JWTs must never be logged (see logging rules in `.claude/CLAUDE.md`).

## Logging

Method-level logging for the Controller, Service, and Repository layers is centralized in a
single Spring AOP aspect rather than hand-written per method:

- **`aspect.LoggingAspect`** (`@Aspect`) wraps every public method in `controller`, `service`,
  and `repository.impl` with a single `@Around` advice. For each call it logs:
  - `ENTER <label> args=[...]` before the call
  - `EXIT <label> durationMs=<n> result=<...>` on success
  - `EXCEPTION <label> durationMs=<n> args=[...] exceptionType=... message=...` on failure, then
    rethrows the exception unchanged (it never wraps or swallows anything)
  - Because layers are nested (Controller → Service → Repository), a single request naturally
    produces a stack of paired ENTER/EXIT lines, one per layer — this is expected, not duplicated
    logging.
- **`aspect.@LoggedOperation("app_schema.object_name")`** — annotate a Repository method with the
  literal Oracle stored procedure/function it calls so the log line shows the real database
  object name (e.g. `app_user.SP_CREATE_USER`) instead of the Java method name. Used on all
  Repository impl methods; Service/Controller methods fall back to `ClassName.methodName`.
- **`util.SensitiveDataMasker`** redacts arguments and return values before they're logged, so
  `LoggingAspect` never needs per-call masking code:
  - Full mask (`***`) for any parameter/field whose name contains `password`, `token`, `jwt`,
    `secret`, `hash`, `sessionid`, `credential`, or `authorization`.
  - Partial mask for anything containing `email` (e.g. `j***@example.com`).
  - Records (all DTOs and repository result types are records) are walked component-by-component
    recursively, so a sensitive field nested inside a response DTO (e.g. `AuthResponse.token()`)
    is still masked — the aspect never calls a domain object's own `toString()`.
  - Collections/lists are summarized as `ClassName[size=N]`, never dumped element-by-element.
- **Exception log level** — a fixed set of expected business exceptions (`ValidationException`,
  `ResourceNotFoundException`, `DuplicateResourceException`, `BadCredentialsException`,
  `UnauthorizedException`, `ForbiddenException`, `BusinessException`) log at `WARN` with no stack
  trace; everything else (including `DatabaseOperationException` and unexpected exceptions) logs
  at `ERROR` with a full stack trace.
- Individual Repository/Service classes no longer hand-write entry/result-code logging — the few
  remaining manual `log.error(...)` calls in `AuthService` and `ExpenseService` are kept
  intentionally, because they capture the raw SQL `result_code` value in a branch that throws a
  generic exception, which the aspect's generic exception log can't reconstruct on its own.
- No `pom.xml` or `log4j2.xml` changes were needed — `spring-boot-starter-aop` was already a
  declared dependency, and Spring Boot auto-configures AOP proxying once it's on the classpath.

## Environment Requirements

- **Java 21** (matches `java.version` in `pom.xml`)
- **Maven** (or the bundled `./mvnw` wrapper, if present in your checkout)
- **Oracle Database** reachable from the app, with the migration scripts above applied
- An Oracle runtime user, normally `EXPENSE_TRACKER`, with `008_grants_runtime_user.sql` applied

### Configuration

`application.yml` is gitignored — copy the template and fill in real values:

```sh
cp src/main/resources/application-example.yml src/main/resources/application.yml
```

The application reads the following environment variables (defaults shown are from
`application-example.yml`; do not rely on defaults for `DB_USER`, `DB_PASSWORD`, or `JWT_SECRET`
outside local development):

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_HOST` | Oracle host | `localhost` |
| `DB_PORT` | Oracle listener port | `1521` |
| `DB_SERVICE_NAME` | Oracle service name | `FREEPDB1` |
| `DB_USER` | Oracle runtime user | `EXPENSE_TRACKER` |
| `DB_PASSWORD` | Oracle password | *(required)* |
| `JWT_SECRET` | HMAC signing secret for JWTs | *(required)* |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |

Multipart upload limits (`max-file-size` / `max-request-size`, currently 5MB) are set in
`application.yml` under `spring.servlet.multipart` and can be adjusted there if larger CSV
imports are required.

## Deployment Steps

1. **Provision Oracle Database** and create/apply the target schemas.
2. **Apply the migration scripts** in `src/main/resources/db/migration/`, in numeric order
   (000 → 009), against that database. Create `EXPENSE_TRACKER` before running `008`; see
   [Updating Tables](#updating-tables) before changing an already-applied schema.
3. **Configure the application**: copy `application-example.yml` to `application.yml` (or set the
   equivalent environment variables directly) and provide real `DB_USER`, `DB_PASSWORD`, and a
   strong, unique `JWT_SECRET`. Never commit real credentials.
4. **Build**:
   ```sh
   mvn clean package
   ```
5. **Run**:
   ```sh
   java -jar target/expense-tracker-0.1.0.jar
   ```
   or, for local development with auto-restart via DevTools:
   ```sh
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
6. **Set the active profile appropriately** for the environment (`dev`/`local` enables the
   Swagger UI without authentication; omit it, or use another profile name, in production).
7. **Point the frontend** at this API's base URL (see `frontend/README.md`,
   `VITE_API_BASE_URL`) and ensure `CORS_ALLOWED_ORIGINS` includes that origin.
8. **Verify**: confirm `/api/auth/register` and `/api/auth/login` succeed, then exercise
   `/api/categories` and `/api/expenses` with the returned Bearer token.

## Development

```sh
mvn spring-boot:run                 # start with default profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # start with Swagger UI enabled
mvn test                            # run tests
mvn clean package                   # build the executable jar
```

With the `dev` or `local` profile active, API documentation is available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
