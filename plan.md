# 個人消費/發票管家 (LINE 發票管家 style) — Implementation Plan

## Context

The repo is currently greenfield — it contains only `.claude/CLAUDE.md` (architecture/convention playbook) and an identical `.codex/AGENTS.md`. No backend, frontend, or database code exists yet. The user wants a "LINE 發票管家"-style personal expense/invoice tracker: record consumption (amount, category, date, invoice number, note), support CSV bulk import of an entire table of records, and guarantee the CSV import is atomic — if any row fails, the **entire batch rolls back** (explicitly requested via `@Transactional`). The project must follow the database-centric architecture mandated by `.claude/CLAUDE.md` (SQL Server owns business logic/validation/transactions; Spring Boot is a thin orchestration + REST layer; Vue 3 + TS frontend consumes the finished API).

Confirmed decisions (via user Q&A):
- **Multi-user with login** — each user only sees their own records.
- **Maven** build tool.
- **SQL Server already exists** — user manages their own connection; we only ship DDL/SP scripts + an `application-example.yml` with env-var placeholders (no docker-compose for DB).
- **CSV columns**: generic custom columns — `expense_date, amount, category, invoice_number, note`.
- **Category model**: fixed lookup table (`app_expense.categories`), not free text — ensures consistent naming/aggregation, frontend gets a dropdown, CSV import validates against it.
- **No email verification / password reset in this pass** — only register/login. Spring Mail dependency stays in the stack per CLAUDE.md but is unused (no `MailService` implementation needed this round).
- **Cross-user ownership failures return 404** (not 403) to avoid ID-enumeration leaks — a deliberate, documented deviation from CLAUDE.md §14's default FORBIDDEN→403 mapping, scoped only to expense ownership checks.
- **Auth mechanism: JWT** (stateless bearer token) — chosen over session cookies because the Vue SPA and Spring Boot API are decoupled/cross-origin in dev; CLAUDE.md §16 requires picking exactly one, not both.

Root package: `com.example.expensetracker`. Repo layout: `backend/` (Maven root), `frontend/` (Vite+Vue3+TS).

---

## Phase 2: SQL Server Design

### Schemas
- `app_user` — users
- `app_expense` — expenses, categories, import batches

### Tables

```sql
-- app_user.users
user_id         bigint IDENTITY(1,1) PRIMARY KEY
email           nvarchar(320) NOT NULL
password_hash   nvarchar(255) NOT NULL
display_name    nvarchar(100) NULL
is_active       bit NOT NULL DEFAULT 1
created_at      datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME()
updated_at      datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME()
CONSTRAINT UQ_users_email UNIQUE (email)

-- app_expense.categories (seeded: 餐飲, 交通, 住宿, 娛樂, 醫療, 其他)
category_id     int IDENTITY(1,1) PRIMARY KEY
name            nvarchar(100) NOT NULL
is_active       bit NOT NULL DEFAULT 1
CONSTRAINT UQ_categories_name UNIQUE (name)

-- app_expense.expenses
expense_id      bigint IDENTITY(1,1) PRIMARY KEY
user_id         bigint NOT NULL REFERENCES app_user.users(user_id)
category_id     int NOT NULL REFERENCES app_expense.categories(category_id)
expense_date    date NOT NULL
amount          decimal(12,2) NOT NULL CHECK (amount > 0)
invoice_number  nvarchar(20) NULL
note            nvarchar(500) NULL
created_at      datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME()
updated_at      datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME()
INDEX IX_expenses_user_date (user_id, expense_date DESC)
INDEX IX_expenses_user_category (user_id, category_id)
CONSTRAINT UQ_expenses_user_invoice UNIQUE (user_id, invoice_number)  -- SQL Server treats multiple NULLs as distinct, safe for optional field

-- app_expense.import_batches (audit row per CSV import attempt)
batch_id        bigint IDENTITY(1,1) PRIMARY KEY
user_id         bigint NOT NULL REFERENCES app_user.users(user_id)
file_name       nvarchar(255) NULL
total_rows      int NOT NULL
success_rows    int NOT NULL
status          nvarchar(20) NOT NULL   -- SUCCESS / FAILED
error_summary   nvarchar(4000) NULL
created_at      datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME()
```

Table type for the CSV bulk import (Table-Valued Parameter):

```sql
CREATE TYPE app_expense.expense_import_row_type AS TABLE (
    row_number      int          NOT NULL,   -- 1-based CSV data row index, for error reporting
    expense_date    date         NULL,       -- NULL if Java couldn't parse it; SP treats as validation error
    amount          decimal(12,2) NULL,
    category_name   nvarchar(100) NULL,
    invoice_number  nvarchar(20)  NULL,
    note            nvarchar(500) NULL
);
```
Java does only safe type coercion (string → date/decimal); anything unparsable passes through as NULL so the stored procedure remains the single source of truth for validation errors (CLAUDE.md §4).

### Stored procedures (schema-qualified, `@result_code`/`@result_message`/`@created_id` convention)

1. `app_user.create_user(@email, @password_hash, @display_name = NULL, @user_id OUTPUT, @result_code OUTPUT, @result_message OUTPUT)` — codes: `SUCCESS`, `DUPLICATE`, `VALIDATION_ERROR`, `SYSTEM_ERROR`. TRY/CATCH + XACT_ABORT, database-owned transaction.
2. `app_user.get_user_by_email(@email, @result_code OUTPUT, @result_message OUTPUT)` — returns result set `{user_id, email, password_hash, display_name, is_active}` via `returningResultSet`; used only by login flow to fetch the hash for BCrypt comparison. Password hash never enters a response DTO.
3. `app_expense.create_expense(@user_id, @expense_date, @amount, @category_id, @invoice_number = NULL, @note = NULL, @expense_id OUTPUT, @result_code OUTPUT, @result_message OUTPUT)` — codes: `SUCCESS`, `VALIDATION_ERROR` (amount ≤ 0 / bad category / bad date), `DUPLICATE` (invoice_number reused by same user), `SYSTEM_ERROR`.
4. `app_expense.update_expense(@expense_id, @user_id, @expense_date, @amount, @category_id, @invoice_number = NULL, @note = NULL, @result_code OUTPUT, @result_message OUTPUT)` — ownership check: row must belong to `@user_id`, else `NOT_FOUND` (mapped externally to HTTP 404 per the ownership-error decision above — never distinguishes "doesn't exist" from "not yours").
5. `app_expense.delete_expense(@expense_id, @user_id, @result_code OUTPUT, @result_message OUTPUT)` — same NOT_FOUND-for-both-cases convention.
6. `app_expense.get_expense_detail(@expense_id, @user_id, @result_code OUTPUT, @result_message OUTPUT)` — result set (0 or 1 row).
7. `app_expense.search_expenses(@user_id, @date_from = NULL, @date_to = NULL, @category_id = NULL, @page = 1, @page_size = 20, @total_count OUTPUT, @result_code OUTPUT, @result_message OUTPUT)` — paginated via `OFFSET/FETCH`, `@page_size` clamped server-side to a max (e.g. 100) so a client can't request unbounded pages. Result set ordered `expense_date DESC, expense_id DESC`, joined to `categories` for `category_name`.
8. `app_expense.v_active_categories` — a **view** (not a proc, per §6 guidance to prefer views for reusable query logic) returning `{category_id, name}` where `is_active = 1`.
9. `app_expense.import_expenses_batch(@user_id, @file_name = NULL, @rows app_expense.expense_import_row_type READONLY, @batch_id OUTPUT, @success_count OUTPUT, @result_code OUTPUT, @result_message OUTPUT)` — the atomic bulk-insert procedure; also returns a second result set `{row_number, error_message}` populated only when the batch is rejected.

### CSV import transaction design (the core requirement)

**Decision: one TVP-based stored procedure call = a database-owned transaction**, not row-by-row Spring-side looping. This directly matches CLAUDE.md §9's "database-owned transaction" pattern and §4's guidance that batch processing belongs in SQL Server.

- `import_expenses_batch` validates **all** rows from the TVP in a single up-front `SELECT` (joins against `categories`, checks `amount > 0`, valid `expense_date`, duplicate `invoice_number` both within the batch via `GROUP BY/HAVING` and against existing DB rows for that user).
- If any row fails: nothing is inserted, `@result_code = 'VALIDATION_ERROR'`, procedure returns the failing rows as `{row_number, error_message}` (safe business messages only, e.g. `"Row 7: category not found"`, never raw SQL error text per §8/§15).
- If all rows pass: single `BEGIN TRAN` inserts every row (`INSERT ... SELECT FROM @rows`) + `COMMIT`, `@result_code = 'SUCCESS'`, `@success_count = N`.
- The `import_batches` audit row is written **after** the rows transaction resolves (commit or rollback), as a separate statement outside that transaction block, so an audit record is always kept regardless of outcome — this sidesteps SQL Server's lack of an autonomous-transaction primitive (§9) without needing a second connection.
- **Where `@Transactional` lives**: `ExpenseImportService.importExpenses(file, userId)` is annotated `@Transactional`, wrapping exactly **one** repository call (`expenseImportRepository.importBatch(...)`, a single `SimpleJdbcCall` to `import_expenses_batch`). Per §5.2 this is technically a case where Spring transaction management isn't strictly required (the SP is self-contained) — it's kept anyway per the user's explicit request and as a defense-in-depth/connection-consistency boundary, not because multiple repository calls need coordinating. This must be documented with a one-line comment in the service explaining why.
- CSV parsing (malformed file / wrong headers / unreadable encoding) happens **before** this transactional boundary and is rejected immediately as HTTP 400 without touching the database — distinct from row-level business validation, which does reach the SP.

### DDL script organization
Plain numbered idempotent `CREATE OR ALTER` scripts under `backend/src/main/resources/db/migration/` (no Flyway/Liquibase — unjustified new dependency given the DB is user-managed and pre-existing):
- `001_schema_app_user.sql`, `002_schema_app_expense.sql` (tables + seed categories), `003_types_app_expense.sql` (TVP type), `004_procs_app_user.sql`, `005_procs_app_expense_crud.sql`, `006_view_active_categories.sql`, `007_proc_import_expenses_batch.sql`

---

## Phase 3: Backend (Maven, `com.example.expensetracker`)

New dependencies beyond the CLAUDE.md-mandated stack (both justified per §36):
- `io.jsonwebtoken:jjwt-api/jjwt-impl/jjwt-jackson` — JWT signing/parsing, standard library, required for the chosen auth mechanism.
- `org.apache.commons:commons-csv` — robust CSV parsing (quoting, embedded commas, BOM), avoids hand-rolled `String.split(",")` bugs.

```
backend/src/main/java/com/example/expensetracker
├── config/        SecurityConfig, CorsConfig, JacksonConfig
├── controller/     AuthController, ExpenseController, CategoryController, ExpenseImportController
├── dto/request/    RegisterRequest, LoginRequest, CreateExpenseRequest, UpdateExpenseRequest
├── dto/response/    AuthResponse, UserResponse, ExpenseResponse, CategoryResponse, PageResponse<T>,
│                    ImportResultResponse{batchId, totalRows, successCount, status, failedRows:[{rowNumber, message}]}
├── exception/      BusinessException, ResourceNotFoundException, DuplicateResourceException,
│                    UnauthorizedException, ForbiddenException, ValidationException,
│                    DatabaseOperationException, GlobalExceptionHandler (@RestControllerAdvice)
├── repository/      UserRepository, ExpenseRepository, ExpenseImportRepository (+ impl/, model/, mapper/)
│                    ExpenseImportRepositoryImpl builds the TVP (SqlStructured) parameter for the batch call
├── security/        JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService, AuthenticatedUser
├── service/          AuthService, ExpenseService, CategoryService, ExpenseImportService
├── constant/         ResultCode, SqlParamNames
└── util/             CsvExpenseParser (tolerant CSV row → typed intermediate object)
```

`src/main/resources/application-example.yml` — committed template, all secrets via env vars (`${DB_HOST}`, `${DB_NAME}`, `${DB_USER}`, `${DB_PASSWORD}`, `${JWT_SECRET}`, `${JWT_EXPIRATION_MS}`). `log4j2.xml` per §13 (no sensitive values logged).

Every expense-related stored procedure call takes the `userId` resolved from the authenticated JWT principal (`AuthenticatedUser`), never a client-supplied user id in the request body — enforced in `ExpenseService`, and re-validated at the SQL layer via the ownership checks in `update_expense`/`delete_expense`.

---

## API Contract

All responses use `{success, message, data}` / `{success, message, errorCode, errors}`. Bearer JWT required except where marked Public.

| Method | Path | Auth | Request | Response `data` | Errors |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | Public | `{email, password, displayName?}` | `{token, user}` | 400, 409 DUPLICATE |
| POST | `/api/auth/login` | Public | `{email, password}` | `{token, user}` | 400, 401 |
| GET | `/api/categories` | Bearer | — | `[{categoryId, name}]` | 401 |
| POST | `/api/expenses` | Bearer | `{expenseDate, amount, categoryId, invoiceNumber?, note?}` | `ExpenseResponse` | 400, 401, 409 |
| PUT | `/api/expenses/{id}` | Bearer | same shape | `ExpenseResponse` | 400, 401, 404 (not found or not owned), 409 |
| DELETE | `/api/expenses/{id}` | Bearer | — | — | 401, 404 |
| GET | `/api/expenses/{id}` | Bearer | — | `ExpenseResponse` | 401, 404 |
| GET | `/api/expenses` | Bearer | query `dateFrom?, dateTo?, categoryId?, page=1, pageSize=20` | `PageResponse<ExpenseResponse>` | 401, 400 |
| POST | `/api/imports/expenses` | Bearer | `multipart/form-data` field `file` (CSV) | `ImportResultResponse` | 400 (bad file/headers), 401, 400 (row-level VALIDATION_ERROR: `status=FAILED`, `successCount=0`, `failedRows` populated) |

CSV contract: header row required (case-insensitive match), UTF-8 (BOM tolerated), columns exactly:
```
expense_date,amount,category,invoice_number,note
2026-07-01,350.00,餐飲,AB12345678,午餐
```
`expense_date` = `yyyy-MM-dd`; `category` matched case-insensitively against `app_expense.categories.name`; `invoice_number`/`note` optional (blank → NULL).

---

## Phase 4: Frontend (Vue 3 + TS + View UI Plus + Pinia + Vue Router + Vite)

```
frontend/src
├── api/            httpClient.ts (JWT attach + 401 handling), authApi, expenseApi, categoryApi, importApi
├── components/      layout/, expense/{ExpenseTable, ExpenseFormModal, ExpenseFilterBar}, import/{CsvUploadPanel, ImportResultTable}
├── composables/     useAuth, usePagination, useApiError, useExpenseFilters
├── stores/           authStore.ts (user, token in localStorage, isAuthenticated getter, login/register/logout)
│                     — no separate expenseStore; list/paging state stays local to ExpenseListView + a composable (only one view needs it, per §23)
├── types/            api/{auth,expense,category,import,common}.ts, domain/{expense,user,category}.ts, form/{expenseForm,loginForm,registerForm}.ts
├── router/           /login, /register public; /expenses, /expenses/import requiresAuth: true; beforeEach guard redirects unauthenticated users to /login
└── views/            auth/{LoginView, RegisterView}, expense/{ExpenseListView, ExpenseImportView}, NotFoundView
```

- Expense form follows CLAUDE.md §29 flow exactly: View UI Plus validation → submitting state → typed API call → success (`Message.success` + refresh list) or normalized inline error, never silently swallowed.
- CSV import view: `Upload` restricted to `.csv`, shows `Spin` while pending, then either a success `Alert` (`successCount`/`totalRows`) or `ImportResultTable` listing every `{rowNumber, message}` — always all-or-nothing (never partial), matching the DB transaction guarantee.

---

## Testing

**Backend**: unit tests for `CsvExpenseParser` (valid/invalid/BOM/missing-header cases), `JwtTokenProvider`, `GlobalExceptionHandler` status mapping. Repository/integration tests against the user's SQL Server (or a disposable test DB) for CRUD result codes, `search_expenses` pagination/filtering, and — critically — an **import rollback test**: 5 valid + 1 invalid row → assert zero rows persisted, `import_batches.status = FAILED`, failing row reported; separately, an all-valid batch → all rows committed atomically. Controller tests for auth/401/404-on-cross-user-access/multipart happy path.

**Frontend**: `vue-tsc --noEmit`, component tests for `ExpenseFormModal` and `CsvUploadPanel` (including the failure-list render branch), router-guard redirect test, manual E2E: register → login → create/edit/delete expense → import a valid CSV → import a CSV with one bad row and confirm 0 rows land in the list with the specific error shown.

---

## Verification

1. Run backend: `mvn spring-boot:run` (after applying the numbered SQL scripts to the user's SQL Server and setting env vars from `application-example.yml`). Confirm `/api/auth/register` + `/api/auth/login` issue a working JWT, and `/api/expenses` CRUD + `/api/expenses` search work via curl/Postman.
2. Confirm the CSV rollback guarantee manually: import a CSV with a deliberately invalid row (bad category or negative amount) mixed with valid rows, verify the API returns `status=FAILED` with the specific failing row, and query `app_expense.expenses` directly to confirm **zero** rows were inserted.
3. Run `npm run dev` on the frontend, exercise login → expense list/filter/pagination → create/edit/delete → CSV import (success and failure cases) in the browser.
4. Run backend unit/integration tests (`mvn test`) and frontend type-check/component tests (`vue-tsc --noEmit`, `vitest`).
