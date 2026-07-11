# Expense Tracker – Backend

Spring Boot 3 (Java 21) REST API for a personal expense/invoice tracker. It follows a
database-centric architecture: business rules, validation, and multi-table workflows live in
SQL Server stored procedures, functions, and views; the Java layers handle HTTP, security, and
orchestration only.

```text
Vue 3 + TypeScript Frontend
        ↓
Spring Boot REST API (Controller → Service → Repository)
        ↓
SQL Server Stored Procedures / Functions / Views
        ↓
SQL Server Tables
```

## Core Features

- **Authentication** (`/api/auth`) – Email/password registration and login. Passwords are hashed
  with BCrypt; `app_user.create_user` and the login lookup enforce email uniqueness and account
  status in the database. A successful login/register returns a JWT (`AuthResponse`) used as a
  stateless Bearer token for all subsequent requests (`SessionCreationPolicy.STATELESS`,
  `JwtAuthenticationFilter`).
- **Expense CRUD** (`/api/expenses`) – Create, update, delete, fetch-by-id, and paginated
  search (filterable by date range and category) for the authenticated user's own expenses.
  Each operation maps to a stored procedure (`app_expense.create_expense`,
  `update_expense`, etc.) that owns validation (positive amount, valid category, duplicate
  invoice number per user) and returns a `result_code`/`result_message` pair that the repository
  translates into typed results and, ultimately, HTTP status codes.
- **Categories** (`/api/categories`) – Read-only list of active expense categories, backed by
  the `app_expense.v_active_categories` view.
- **CSV batch import** (`/api/imports/expenses`) – Multipart upload of a CSV file
  (`expense_date, amount, category, invoice_number, note` columns) to bulk-create expenses.
  `CsvExpenseParser` handles structural parsing only (headers, type coercion, BOM/UTF-8); all
  business validation happens in `app_expense.import_expenses_batch`, which is **all-or-nothing**
  — if any row fails, the whole batch is rolled back and the failing rows are returned with
  reasons, while an audit row is still written to `import_batches` either way.
- **Standardized API responses** – Every endpoint returns the same envelope
  (`{ "success", "message", "data" }` on success; `{ "success": false, "message", "errorCode",
  "errors" }` on failure) via a `@RestControllerAdvice` global exception handler that maps
  business exceptions to HTTP status codes without leaking SQL Server internals.
- **API documentation** – springdoc-openapi is wired up; Swagger UI and the OpenAPI JSON are
  exposed only under the `dev`/`local` Spring profiles (see [Security](#security) below).

## Architecture / Package Layout

```text
src/main/java/com/example/expensetracker
├── config          # Security, CORS, OpenAPI wiring
├── constant        # Result-code and SQL parameter-name constants
├── controller       # HTTP endpoints only — no SQL, no business logic
├── dto
│   ├── request     # Validated inbound DTOs
│   └── response    # Outbound DTOs (ApiResponse envelope, PageResponse, etc.)
├── exception        # Business exceptions + global exception handler
├── repository        # JdbcTemplate/SimpleJdbcCall calls into stored procs/views
│   ├── model        # Raw repository result shapes (not exposed to clients)
│   ├── mapper        # RowMapper implementations
│   └── impl
├── security          # JWT provider/filter, authenticated-principal resolution
├── service           # Orchestration, security context, DTO translation
└── util              # CSV parsing helper

src/main/resources
├── application.yml           # Local config (gitignored — copy from the example below)
├── application-example.yml   # Template; copy to application.yml and fill in real values
├── log4j2.xml
└── db/migration/             # Numbered SQL Server DDL/stored-procedure scripts (see below)
```

## Database Objects

SQL Server schemas group related objects, applied in order from `src/main/resources/db/migration/`:

| File | Purpose |
| --- | --- |
| `001_schema_app_user.sql` | `app_user` schema and `users` table |
| `002_schema_app_expense.sql` | `app_expense` schema, `categories`, `expenses`, `import_batches` tables |
| `003_types_app_expense.sql` | `app_expense.expense_import_row_type` table type (TVP for batch import) |
| `004_procs_app_user.sql` | `app_user.create_user` and related user procedures |
| `005_procs_app_expense_crud.sql` | `app_expense.create_expense`, `update_expense`, `delete_expense`, `get_expense`, `search_expenses` |
| `006_view_active_categories.sql` | `app_expense.v_active_categories` view |
| `007_proc_import_expenses_batch.sql` | `app_expense.import_expenses_batch` — atomic CSV bulk import |
| `008_fix_expenses_invoice_null_unique.sql` | Filtered unique index fix so multiple expenses with no invoice number are allowed per user |

These scripts are **not** run automatically (no Flyway/Liquibase dependency is wired in) — apply
them manually and in numeric order against your SQL Server database before starting the app,
e.g. with `sqlcmd` or SQL Server Management Studio.

Stored procedures follow a consistent result convention: business outcomes are reported via
`@result_code` / `@result_message` output parameters (`SUCCESS`, `VALIDATION_ERROR`, `NOT_FOUND`,
`DUPLICATE`, `SYSTEM_ERROR`, ...), which the repository layer maps to Java exceptions and the
global exception handler maps to HTTP status codes.

## Security

- Stateless JWT authentication (`io.jsonwebtoken` / jjwt) — no server-side session state.
- `/api/auth/**` is public; every other endpoint requires a valid Bearer token.
- Swagger UI (`/swagger-ui.html`) and the OpenAPI doc (`/api-docs`) are only exposed when the
  active Spring profile is `dev` or `local` — they are locked down (require auth) otherwise.
- CORS allowed origins are configurable via `app.cors.allowed-origins` (comma-separated).
- Passwords are hashed with `BCryptPasswordEncoder`; plaintext passwords, password hashes, and
  full JWTs must never be logged (see logging rules in `.claude/CLAUDE.md`).

## Environment Requirements

- **Java 21** (matches `java.version` in `pom.xml`)
- **Maven** (or the bundled `./mvnw` wrapper, if present in your checkout)
- **Microsoft SQL Server** reachable from the app, with the migration scripts above applied
- A SQL Server login with rights to the `app_user` / `app_expense` schemas

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
| `DB_HOST` | SQL Server host | `localhost` |
| `DB_PORT` | SQL Server port | `1433` |
| `DB_NAME` | Database name | `expense_tracker` |
| `DB_ENCRYPT` | Enable encrypted JDBC connection | `true` |
| `DB_TRUST_CERT` | Trust the server certificate (dev convenience only) | `true` |
| `DB_USER` | SQL Server login | *(required)* |
| `DB_PASSWORD` | SQL Server password | *(required)* |
| `JWT_SECRET` | HMAC signing secret for JWTs | *(required)* |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |

Multipart upload limits (`max-file-size` / `max-request-size`, currently 5MB) are set in
`application.yml` under `spring.servlet.multipart` and can be adjusted there if larger CSV
imports are required.

## Deployment Steps

1. **Provision SQL Server** and create the target database (`DB_NAME`).
2. **Apply the migration scripts** in `src/main/resources/db/migration/`, in numeric order
   (001 → 008), against that database.
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
