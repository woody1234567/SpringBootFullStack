# Project Context for Coding Agents

You are an AI coding agent working on an enterprise full-stack web application.

Before modifying code, first inspect the existing project structure, coding conventions, database integration patterns, and API response formats.

Do not introduce a new framework, ORM, architectural pattern, or dependency unless explicitly requested.

---

# 1. Core Development Principles

This project follows a database-centric architecture.

The primary implementation order is:

1. Analyze the business requirement.
2. Design or modify the Oracle database objects.
3. Implement business logic in PL/SQL Packages, Stored Procedures, Functions, Views, or Triggers when justified.
4. Implement the Spring Boot Repository integration.
5. Implement the Spring Boot Service and REST API.
6. Test and confirm the backend API contract.
7. Implement the frontend TypeScript types and API client.
8. Implement the Vue frontend interface based on the completed API.
9. Perform frontend and backend integration testing.

The backend API must be completed and stabilized before implementing the frontend interface.

The frontend must follow the backend API contract. Do not design frontend mock data structures that conflict with the actual backend response.

---

# 2. System Architecture

The project uses the following architecture:

```text
Vue 3 + TypeScript Frontend
        ↓
Spring Boot REST API
        ↓
Service Layer
        ↓
Repository Layer
        ↓
Oracle Schema / Package / Procedure / Function / View
        ↓
Oracle Tables
```

Business rules should be implemented primarily in Oracle Database.

The responsibilities of each layer must remain clear.

---

# 3. Technology Stack

## 3.1 Frontend

* Vue 3
* TypeScript
* View UI Plus, formerly iView
* Pinia
* Vue Router
* Axios or the project's existing HTTP client
* Vite, if already used by the project

## 3.2 Backend

* Java
* Spring Boot
* Spring Web
* Spring Security
* Spring JDBC
* Spring Validation
* Spring AOP
* Spring Mail
* Spring Boot DevTools
* Jakarta APIs
* Lombok
* Log4j2
* Oracle JDBC Driver (`com.oracle.database.jdbc:ojdbc11`)

## 3.3 Database

* Oracle Database
* PL/SQL
* Database Schema
* Package (specification and body)
* Stored Procedure
* Scalar Function
* Table Function, pipelined only when justified
* Table
* View
* `IDENTITY` column or `SEQUENCE`
* Constraints and Indexes
* Object types and nested table (collection) types when necessary

---

# 4. Database-Centric Business Logic

Business logic must be implemented primarily in Oracle Database.

Use Oracle database objects for:

* Business validation
* Business calculations
* Data consistency rules
* Multi-table data updates
* Complex query logic
* Workflow state transitions
* Permission-related database checks
* Batch processing
* Business identifier generation
* Business status determination
* Data aggregation
* Data transformation
* Transactional data operations

Preferred Oracle organization:

```text
Schema: app_user
└── Package: PG_USER
    ├── Procedure: SP_CREATE_USER
    ├── Procedure: SP_UPDATE_USER
    ├── Procedure: SP_VERIFY_EMAIL
    ├── Procedure: SP_RESET_PASSWORD
    ├── Procedure: SP_GET_USER_DETAIL   (returns a SYS_REFCURSOR OUT parameter)
    └── Function:  FN_EMAIL_EXISTS
```

Use Oracle schemas as module boundaries (for example `app_user`, `app_expense`), and use Packages to group the procedures and functions of each module. Oracle Packages support a specification, a body, and private package members; prefer Packages over loose standalone procedures.

Prefer schema-qualified object names such as:

```sql
app_user.PG_USER.SP_CREATE_USER
app_order.PG_ORDER.SP_CREATE_ORDER
security.PG_SECURITY.FN_CHECK_PERMISSION
```

Use Stored Procedures for commands, multi-step workflows, and operations requiring output parameters or transaction control.

Prefer Views or table functions for reusable query logic. Use scalar functions carefully because row-by-row SQL-to-PL/SQL context switching may reduce performance in large queries.

The Java backend should not duplicate business rules already implemented in Oracle.

Do not copy PL/SQL business validation into the Java Service layer unless it is required for HTTP request validation, security protection, or early input rejection.

## Naming Convention

All Oracle database object names must follow the project skill `oracle-naming-convention` (`.claude/skills/oracle-naming-convention/`):

* UPPER_SNAKE_CASE, singular nouns, names <= 60 characters.
* Mandatory type prefixes: `TB_` table, `VW_` view, `IX_` index, `PK_` primary key, `UK_` unique key, `FK_` foreign key, `SP_` procedure, `FN_` function, `PG_` package, `SQ_` sequence, `TO_` object type, `TT_` table type, `MV_` materialized view, `JB_` job, `TR_` trigger, `DR_` directory.
* Constraint and index names align with the owning table (`TB_USER` → `PK_USER`, `UK_USER_1`, `IX_USER_1`).

Never invent new prefixes, never omit prefixes, and never mix naming styles.

---

# 5. Responsibility of Each Backend Layer

## 5.1 Controller Layer

The Controller layer is responsible for:

* Receiving HTTP requests
* Mapping request parameters
* Validating request DTOs
* Calling the Service layer
* Returning standardized API responses
* Mapping HTTP status codes

Controllers must not contain:

* SQL
* JdbcTemplate calls
* SimpleJdbcCall calls
* Business calculations
* Oracle-specific parameter handling
* Complex permission logic
* Transactional database workflows

Example package:

```text
controller
```

---

## 5.2 Service Layer

The Service layer is a thin application orchestration layer.

It is responsible for:

* Coordinating Repository calls
* Managing application-level transactions
* Applying Spring Security authorization
* Retrieving authenticated user information
* Converting Repository results into response DTOs
* Calling email services
* Handling external integrations
* Translating database errors into application exceptions
* Managing API-level workflows

The Service layer should not duplicate Oracle business logic.

Use `@Transactional` on Service methods when multiple Repository calls must participate in the same Java-managed transaction.

Do not add `@Transactional` automatically to every method.

For a single Oracle Stored Procedure that already controls the complete database operation, evaluate whether Spring transaction management is necessary before adding it.

Example package:

```text
service
```

---

## 5.3 Repository Layer

The Repository layer is responsible for database communication.

Use:

* `JdbcTemplate`
* `NamedParameterJdbcTemplate`
* `SimpleJdbcCall`

The Repository layer may:

* Call Oracle Package procedures and standalone Stored Procedures
* Call Oracle Functions
* Execute SQL queries
* Map query results
* Register Oracle input and output parameters, including `SYS_REFCURSOR` output parameters
* Map result sets to Java objects
* Convert Oracle database results into repository result objects
* Translate low-level database exceptions when appropriate

The Repository layer must not contain business logic.

Repository methods should reflect database operations clearly.

Good examples:

```java
createUser(...)
verifyEmail(...)
resetPassword(...)
getUserDetail(...)
checkEmailExists(...)
```

Avoid generic method names such as:

```java
execute(...)
run(...)
call(...)
process(...)
```

unless the method is an internal reusable helper.

Example package:

```text
repository
```

---

# 6. Oracle Stored Procedure and Function Integration

Use `SimpleJdbcCall` for Oracle Stored Procedures when it fits the existing project style.

For a procedure inside a Package, declare the package name with `withCatalogName` — this is the standard Spring JDBC convention for Oracle Packages:

```java
SimpleJdbcCall createUserCall = new SimpleJdbcCall(jdbcTemplate)
        .withSchemaName("APP_USER")
        .withCatalogName("PG_USER")
        .withProcedureName("SP_CREATE_USER");
```

When JDBC metadata lookup is unreliable or causes startup or execution overhead, explicitly declare parameters.

Example:

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

PL/SQL parameter names have no `@` prefix. This project uses the `p_` prefix for procedure and function parameters, and Spring JDBC map keys must match the declared PL/SQL parameter names exactly (for example `p_email`). Keep this convention consistent throughout the project.

Oracle returns ordinary result sets through a `SYS_REFCURSOR` output parameter. Map the cursor with `SqlOutParameter` and `OracleTypes.CURSOR`:

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

Do not rely on implicit result sets (`DBMS_SQL.RETURN_RESULT`) unless the project explicitly documents that pattern; prefer an explicit `SYS_REFCURSOR` OUT parameter.

Call scalar functions with `JdbcTemplate` or `NamedParameterJdbcTemplate` using `SELECT ... FROM dual`:

```java
Boolean exists = jdbcTemplate.queryForObject(
        "SELECT app_user.PG_USER.FN_EMAIL_EXISTS(?) FROM dual",
        Boolean.class,
        email
);
```

A function called from SQL cannot return a PL/SQL `BOOLEAN`. Boolean-style functions must return `NUMBER(1)` with values `0` and `1`, matching the project's existing boolean column convention.

Call table functions with `SELECT * FROM TABLE(...)`:

```java
List<UserRow> users = namedParameterJdbcTemplate.query(
        "SELECT * FROM TABLE(app_user.PG_USER.FN_SEARCH_USERS(:p_keyword))",
        Map.of("p_keyword", keyword),
        userRowMapper
);
```

Use named constants for parameter names when the same parameters are reused.

```java
private static final String P_EMAIL = "p_email";
private static final String P_RESULT_CODE = "p_result_code";
```

Do not scatter raw Oracle parameter names across multiple classes.

---

# 7. Oracle Result Convention

Oracle Stored Procedures should return predictable result information.

Recommended output parameters:

```text
p_result_code       OUT VARCHAR2(50)
p_result_message    OUT VARCHAR2(4000)
```

For object creation operations, additionally return:

```text
p_created_id        OUT NUMBER
```

For query operations, prefer a `SYS_REFCURSOR` output parameter opened with a normal `SELECT`. For reusable pure-query logic, prefer a View or a table function that the Repository can query directly.

Recommended semantic convention:

```text
SUCCESS
VALIDATION_ERROR
NOT_FOUND
DUPLICATE
FORBIDDEN
SYSTEM_ERROR
```

Alternatively, use a documented numeric code convention if the existing project already uses numeric result codes.

Do not mix unrelated result code styles within the same project.

Do not use a function `RETURN` value to carry business result codes when output parameters are the documented convention. Keep result codes, output parameters, and cursor result sets clearly separated.

The Repository or Service layer must map Oracle result codes into appropriate Java exceptions and HTTP responses.

---

# 8. Oracle Exception Handling

Oracle Stored Procedures must handle expected business conditions explicitly and use an `EXCEPTION` block for unexpected database errors.

Example:

```sql
CREATE OR REPLACE PACKAGE app_user.PG_USER AS

    PROCEDURE SP_CREATE_USER (
        p_email          IN  VARCHAR2,
        p_password_hash  IN  VARCHAR2,
        p_user_id        OUT NUMBER,
        p_result_code    OUT VARCHAR2,
        p_result_message OUT VARCHAR2
    );

END PG_USER;
/

CREATE OR REPLACE PACKAGE BODY app_user.PG_USER AS

    PROCEDURE SP_CREATE_USER (
        p_email          IN  VARCHAR2,
        p_password_hash  IN  VARCHAR2,
        p_user_id        OUT NUMBER,
        p_result_code    OUT VARCHAR2,
        p_result_message OUT VARCHAR2
    ) IS
        v_count PLS_INTEGER;
    BEGIN
        SELECT COUNT(*) INTO v_count
        FROM app_user.TB_USER
        WHERE email = p_email;

        IF v_count > 0 THEN
            p_result_code := 'DUPLICATE';
            p_result_message := 'Email already exists';
            RETURN;
        END IF;

        INSERT INTO app_user.TB_USER (email, password_hash)
        VALUES (p_email, p_password_hash)
        RETURNING user_id INTO p_user_id;

        p_result_code := 'SUCCESS';
        p_result_message := 'User created successfully';
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            p_result_code := 'DUPLICATE';
            p_result_message := 'Email already exists';
        WHEN OTHERS THEN
            p_result_code := 'SYSTEM_ERROR';
            p_result_message := 'Unable to complete the database operation';
            RAISE;
    END SP_CREATE_USER;

END PG_USER;
/
```

For duplicate key violations, handle `DUP_VAL_ON_INDEX` (ORA-00001) when the operation requires a specific duplicate response. Prefer enforcing uniqueness with a unique constraint or index rather than relying only on a preliminary existence check.

Use these Oracle functions inside `EXCEPTION` blocks when detailed server-side logging is required:

```text
SQLCODE
SQLERRM
DBMS_UTILITY.FORMAT_ERROR_STACK
DBMS_UTILITY.FORMAT_ERROR_BACKTRACE
```

Do not expose sensitive database details, schema names, SQL statements, stack traces, password hashes, or internal implementation information through API responses.

Log detailed errors on the backend or in an approved audit mechanism, but return safe user-facing messages.

---

# 9. Transaction Rules

Business transactions should have a clearly defined owner.

## Database-owned transaction

Use this when one Stored Procedure performs the complete database workflow and is intentionally responsible for its own transaction.

Example:

```text
app_order.PG_ORDER.SP_CREATE_ORDER
```

The Stored Procedure may:

* Validate inventory
* Insert order
* Insert order items
* Update inventory
* Create audit records

Recommended PL/SQL pattern:

```sql
BEGIN
    -- Complete business workflow.

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
```

## Spring-owned transaction

Use this when one Service method coordinates multiple Repository operations, or when transaction ownership belongs to the application layer.

In this mode, do not put `COMMIT` or `ROLLBACK` statements inside called Stored Procedures. Let the JDBC connection and Spring transaction manager control the outer transaction.

Oracle has no nested transactions. A Stored Procedure that may run inside an existing transaction must not blindly roll back work it does not own. When a procedure needs nested-safe behavior, declare a `SAVEPOINT`, roll back to that savepoint on failure, and document the strategy.

Use `@Transactional` only where an application-level transaction boundary is genuinely required.

Avoid ambiguous transaction ownership.

Oracle supports `PRAGMA AUTONOMOUS_TRANSACTION` for independent persistence such as audit logging that must survive a rollback of the main transaction. An autonomous block must issue its own `COMMIT` or `ROLLBACK`. Use it sparingly, only for genuinely independent writes, and document every autonomous transaction explicitly.

---

# 10. Backend Package Structure

Use a structure similar to:

```text
src/main/java/com/example/project
├── config
├── controller
├── dto
│   ├── request
│   └── response
├── exception
├── repository
│   ├── model
│   ├── mapper
│   └── impl
├── security
├── service
├── mail
├── aspect
├── constant
└── util
```

Adjust the root package to match the existing project.

Do not reorganize the entire project unless explicitly requested.

---

# 11. DTO Rules

Use separate DTOs for API requests and responses.

Example:

```text
dto/request/CreateUserRequest.java
dto/response/UserResponse.java
```

Do not expose:

* Oracle parameter maps
* Raw JDBC result-set structures
* Database row objects
* Password hashes
* Security tokens not intended for clients
* Internal database result codes unless part of the documented API

Use Jakarta Validation annotations.

Example:

```java
@Data
@Builder
public class CreateUserRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}
```

Use Lombok annotations correctly:

* `@Data`
* `@Getter`
* `@Setter`
* `@Builder`
* `@RequiredArgsConstructor`
* `@Slf4j`

The correct Lombok annotation is `@Data`, not `@Date`.

Prefer more specific Lombok annotations over `@Data` for security-sensitive or immutable classes.

---

# 12. Dependency Injection

Prefer constructor injection.

Recommended:

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
}
```

Avoid field injection:

```java
@Autowired
private UserRepository userRepository;
```

---

# 13. Logging

Use Lombok `@Slf4j`.

Log:

* Operation start when useful
* Important business identifiers
* Oracle Package, Stored Procedure, or Function name
* Result code
* Execution failure
* Unexpected exception
* Security-sensitive events

Do not log:

* Plaintext passwords
* Password hashes
* Complete JWT tokens
* Session IDs
* Email verification tokens
* Password reset tokens
* Confidential personal information
* Sensitive database connection information

Example:

```java
log.info("Calling PG_USER.SP_CREATE_USER for email={}", maskedEmail);
```

Use Log4j2 as the logging implementation.

---

# 14. Global Exception Handling

Use `@RestControllerAdvice`.

Define clear application exceptions such as:

```text
BusinessException
ResourceNotFoundException
DuplicateResourceException
UnauthorizedException
ForbiddenException
DatabaseOperationException
ValidationException
```

Map exceptions to appropriate HTTP status codes.

Example:

```text
Validation error      → 400 Bad Request
Authentication error  → 401 Unauthorized
Authorization error   → 403 Forbidden
Not found             → 404 Not Found
Duplicate resource    → 409 Conflict
Unexpected error      → 500 Internal Server Error
```

---

# 15. API Response Format

Use a consistent JSON response structure.

Success response:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {}
}
```

Error response:

```json
{
  "success": false,
  "message": "Unable to complete the operation",
  "errorCode": "USER_NOT_FOUND",
  "errors": []
}
```

Do not return raw Oracle exceptions or `ORA-` error messages to the frontend.

Keep response formats consistent across all controllers.

---

# 16. Spring Security

The system supports:

* Email and password authentication
* JWT authentication or Session authentication
* Role-based authorization
* Email verification
* Password reset

Do not implement JWT and Session authentication simultaneously unless the requirement explicitly calls for both.

Before implementing authentication, determine which mode the existing project uses.

## Authentication flow

Typical flow:

```text
Frontend
    ↓
Authentication API
    ↓
Spring Security
    ↓
Service
    ↓
Repository
    ↓
Oracle Stored Procedure
```

Oracle may handle:

* User lookup
* Account status
* Role lookup
* Email verification status
* Failed login counter
* Account locking
* Password reset state
* Token record management

Spring Security must handle:

* Authentication context
* Request filters
* Endpoint protection
* Role and authority mapping
* JWT validation or Session management
* CSRF configuration
* Password encoding
* Security exception responses

Passwords must be hashed using a secure password encoder such as BCrypt or the project's configured encoder.

Never store plaintext passwords.

---

# 17. Email Verification and Password Reset

Use Spring Mail for sending emails.

The database may manage:

* Verification token records
* Reset token records
* Token expiry time
* Token usage status
* User verification status

The backend is responsible for:

* Generating secure random tokens
* Hashing tokens before database storage when appropriate
* Sending email
* Validating API requests
* Calling the related Oracle Stored Procedure
* Returning a safe response

Do not reveal whether an email account exists during password reset unless the project requirement explicitly permits it.

---

# 18. Frontend Architecture

The frontend uses Vue 3 with TypeScript.

Recommended source structure:

```text
src
├── api
├── assets
├── components
│   ├── common
│   ├── form
│   └── layout
├── composables
├── constants
├── layouts
├── router
├── stores
├── types
│   ├── api
│   ├── domain
│   └── form
├── utils
├── views
└── App.vue
```

Feature-based organization may be used for larger modules:

```text
src/features/user
├── api
├── components
├── composables
├── stores
├── types
└── views
```

Follow the existing project structure when one already exists.

Do not restructure the entire frontend without explicit approval.

---

# 19. TypeScript Rules

Use TypeScript for all new frontend code.

Prefer:

```vue
<script setup lang="ts">
```

Avoid:

* `any`
* Untyped API responses
* Duplicated interface definitions
* Implicitly shaped objects
* Direct access to unknown error objects
* Large components with mixed responsibilities

Use:

* `interface` for object contracts
* `type` for unions, aliases, and utility compositions
* Generic API response types
* Typed component props
* Typed emits
* Typed Pinia state
* Typed composable return values

Example:

```typescript
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}
```

Example:

```typescript
export interface User {
  id: number
  email: string
  role: UserRole
  emailVerified: boolean
}
```

Example:

```typescript
export type UserRole = 'ADMIN' | 'USER'
```

Do not use `any` to bypass TypeScript errors.

Use `unknown` and perform type narrowing when the actual type is not known.

---

# 20. Frontend API Layer

All HTTP calls must be placed in the API layer.

Do not call Axios directly from page components unless the existing project explicitly follows that pattern.

Recommended:

```text
src/api/httpClient.ts
src/api/authApi.ts
src/api/userApi.ts
```

Example:

```typescript
export const createUser = (
  payload: CreateUserRequest
): Promise<ApiResponse<UserResponse>> => {
  return httpClient.post('/api/users', payload)
}
```

The API layer is responsible for:

* API endpoint definitions
* Request and response typing
* Query parameter serialization
* HTTP client invocation
* Basic response normalization

The API layer should not contain:

* UI state
* View navigation
* Modal behavior
* Toast presentation logic
* Complex business logic

---

# 21. HTTP Client Configuration

Create a shared HTTP client.

It may handle:

* Base URL
* Authorization header
* Request timeout
* Credentials for Session authentication
* JWT attachment
* Standard error conversion
* Unauthorized response handling
* Request correlation ID if required

Example:

```typescript
const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
  withCredentials: true
})
```

For JWT authentication, attach the token according to the project's security design.

For Session authentication, use `withCredentials: true` when required.

Do not implement token storage before confirming the backend authentication strategy.

---

# 22. Frontend Type Organization

Separate API types, domain types, and form types.

Example:

```text
types/api/auth.ts
types/api/user.ts
types/domain/user.ts
types/form/loginForm.ts
```

API request type:

```typescript
export interface LoginRequest {
  email: string
  password: string
}
```

API response type:

```typescript
export interface LoginResponse {
  user: User
  accessToken?: string
}
```

Frontend form state may differ from API types when necessary.

Example:

```typescript
export interface PasswordResetForm {
  password: string
  confirmPassword: string
}
```

Do not force form-only fields into backend API DTOs.

---

# 23. Pinia State Management

Use Pinia for shared application state.

Suitable Pinia state includes:

* Authenticated user
* Authentication status
* Roles and permissions
* Global application preferences
* Shared reference data
* Data used across multiple unrelated views

Do not place every page variable in Pinia.

Local component state should remain local when it is only used by one component or one view.

Recommended store structure:

```text
stores/authStore.ts
stores/userStore.ts
stores/appStore.ts
```

Use typed state, getters, and actions.

Example:

```typescript
export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    initialized: false
  }),

  getters: {
    isAuthenticated: state => state.user !== null,
    isAdmin: state => state.user?.role === 'ADMIN'
  },

  actions: {
    async fetchCurrentUser(): Promise<void> {
      // Call the API layer.
    }
  }
})
```

Pinia actions may coordinate API calls and shared state.

Do not place View UI Plus modal code or direct DOM operations inside stores.

---

# 24. Vue Component Design

Separate components by responsibility.

Page components are responsible for:

* Page-level layout
* Calling composables or stores
* Coordinating child components
* Handling route parameters
* Displaying loading, empty, and error states

Reusable components are responsible for:

* Rendering UI
* Receiving typed props
* Emitting typed events
* Avoiding direct API calls when possible

Example:

```vue
<script setup lang="ts">
interface Props {
  user: User
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  edit: [userId: number]
  delete: [userId: number]
}>()
</script>
```

Avoid creating extremely large `.vue` files.

When a component becomes difficult to understand, extract:

* Reusable UI components
* Composables
* Type definitions
* API calls
* Utility functions

---

# 25. Composables

Use composables for reusable stateful frontend logic.

Examples:

```text
useAuth
usePagination
usePermission
useFormValidation
useApiError
```

A composable may manage:

* Loading state
* Error state
* Reusable API workflow
* Pagination
* Route-related behavior
* Form submission workflow

Example:

```typescript
export function useApiError() {
  const getErrorMessage = (error: unknown): string => {
    // Safely narrow and normalize the error.
    return 'An unexpected error occurred'
  }

  return {
    getErrorMessage
  }
}
```

Do not use composables as a replacement for every utility function.

Stateless helpers belong in `utils`.

---

# 26. View UI Plus Rules

Use View UI Plus as the primary UI component library.

Prefer existing View UI Plus components before creating custom replacements.

Use components consistently for:

* Form
* FormItem
* Input
* Select
* Button
* Table
* Modal
* Message
* Notice
* Pagination
* DatePicker
* Checkbox
* Radio
* Dropdown
* Spin
* Alert

Keep presentation behavior in Vue components.

Do not trigger View UI Plus notifications from Repository-like frontend API modules.

Use centralized form validation rules where appropriate.

Example:

```typescript
const rules: FormRules = {
  email: [
    {
      required: true,
      message: 'Email is required',
      trigger: 'blur'
    }
  ]
}
```

Frontend validation improves user experience, but it does not replace backend validation or Oracle business validation.

---

# 27. Frontend Permission Control

Frontend permission checks are for user experience only.

They may be used to:

* Hide unauthorized menu items
* Disable actions
* Prevent navigation to restricted pages
* Display role-specific content

The backend must always enforce authorization independently.

Do not rely on frontend role checks for security.

Create a reusable permission utility or composable.

Example:

```typescript
const { hasRole, hasPermission } = usePermission()
```

Use Vue Router guards for route-level access control.

---

# 28. Vue Router

Use route metadata for authentication and authorization requirements.

Example:

```typescript
{
  path: '/admin/users',
  component: () => import('@/views/admin/UserListView.vue'),
  meta: {
    requiresAuth: true,
    roles: ['ADMIN']
  }
}
```

Router guards may:

* Check authentication initialization
* Redirect unauthenticated users
* Check role requirements
* Prevent access to restricted routes

The router must not be treated as a security boundary. Spring Security remains the actual security boundary.

---

# 29. Form Implementation

Forms should include:

* Typed form state
* View UI Plus validation
* Loading state
* Disabled state during submission
* Backend validation error display
* Safe error normalization
* Success feedback
* Duplicate submission prevention

Example flow:

```text
User submits form
    ↓
Frontend validation
    ↓
Set submitting state
    ↓
Call typed API function
    ↓
Handle backend response
    ↓
Update store or navigate
    ↓
Clear submitting state
```

Do not silently ignore backend errors.

---

# 30. Loading, Empty, and Error States

Every data-driven page should explicitly handle:

* Initial loading
* Refresh loading
* Empty result
* API error
* Permission denied
* Successful result

Do not display an empty table without explaining whether data is loading, unavailable, or genuinely empty.

---

# 31. API-First Frontend Development

Frontend implementation begins only after the backend API is available or its contract has been explicitly finalized.

Before implementing a frontend feature, obtain or define:

* Endpoint
* HTTP method
* Request path parameters
* Query parameters
* Request body
* Response body
* Error response
* Authentication requirement
* Role requirement
* Pagination behavior
* Date and time format
* Nullable fields

Create TypeScript interfaces based on the real backend DTOs.

Do not guess field names.

Do not invent response properties without updating the backend contract.

When backend and frontend fields differ, document and implement an explicit mapping.

---

# 32. Backend-First Feature Workflow

For every new feature, use the following workflow.

## Phase 1: Requirement Analysis

Identify:

* Business goal
* User role
* Input data
* Output data
* Business rules
* Validation rules
* Database changes
* Security requirements
* Email or notification requirements

## Phase 2: Oracle Design

Define:

* Tables or table changes
* Indexes
* Constraints
* `SEQUENCE` or `IDENTITY` columns
* Views
* Package specifications
* Stored Procedure signatures
* Stored Procedure implementations
* Procedure inputs
* Procedure outputs, including `SYS_REFCURSOR` parameters
* Function return values
* Result set structures
* Result codes
* Transaction behavior
* Exception behavior

## Phase 3: Backend Integration

Implement:

* Repository method
* SimpleJdbcCall configuration
* Oracle parameter mapping
* Result set mapping
* Service orchestration
* Security checks
* Request DTO
* Response DTO
* Controller endpoint
* Exception mapping
* Logging
* Tests

## Phase 4: API Verification

Confirm:

* Request contract
* Response contract
* HTTP status
* Authentication behavior
* Authorization behavior
* Validation behavior
* Oracle result code mapping
* Error response
* API documentation

## Phase 5: Frontend Implementation

Implement:

* TypeScript API request types
* TypeScript API response types
* API client function
* Pinia state when shared state is required
* Composable when reusable workflow is required
* Vue page
* Reusable components
* View UI Plus validation
* Loading state
* Empty state
* Error state
* Permission control
* Router integration

## Phase 6: Integration Testing

Verify:

* Successful flow
* Validation failure
* Unauthorized request
* Forbidden request
* Record not found
* Duplicate data
* Oracle exception
* Network error
* Repeated submission
* Expired authentication
* Frontend and backend type consistency

---

# 33. Testing Requirements

For backend implementation, consider:

* Unit tests for Java mapping and application logic
* Integration tests for Repository calls
* API tests for Controllers
* Security tests
* Oracle Stored Procedure and PL/SQL tests
* Transaction rollback tests
* Validation tests

For frontend implementation, consider:

* Type checking
* Component tests
* Store tests
* API error handling
* Route guard tests
* Form validation tests
* Loading and empty state tests

Do not claim that code has been tested unless the tests were actually executed.

---

# 34. Coding Agent Behavior

Before implementing a feature:

1. Inspect the relevant files.
2. Identify the existing coding conventions.
3. Identify the related Oracle objects.
4. Determine whether similar functionality already exists.
5. Explain the implementation plan.
6. List the files and database objects to create or modify.
7. Implement the Oracle business logic first.
8. Implement the Spring Boot backend.
9. Verify the API contract.
10. Implement the Vue TypeScript frontend.
11. Run or describe relevant tests.
12. Summarize the changes and remaining risks.

Do not start with frontend implementation when the backend API does not yet exist.

Do not create mock endpoints as a substitute for the requested backend implementation unless explicitly requested.

---

# 35. Output Format for Feature Requests

When asked to implement a feature, respond in this order:

## 1. Requirement Understanding

Briefly describe the requested behavior and relevant assumptions.

## 2. Implementation Plan

Explain the Oracle, backend, and frontend changes.

## 3. Files and Database Objects

List files and Oracle objects to create or modify.

## 4. Oracle Implementation

Provide:

* DDL when required
* Package and Stored Procedure signatures
* Stored Procedure implementations
* Procedure or Function implementation
* Result codes
* Exception handling

## 5. Spring Boot Implementation

Provide:

* Request DTO
* Response DTO
* Repository
* Service
* Controller
* Security configuration when required
* Exception handling
* Logging

## 6. API Contract

Document:

* HTTP method
* Endpoint
* Request
* Response
* Error cases
* Authentication
* Required roles

## 7. Vue TypeScript Implementation

Provide:

* TypeScript types
* API function
* Pinia store when required
* Composable when required
* Vue view
* Reusable components
* Router updates
* Permission handling

## 8. Tests

Provide or update relevant tests.

## 9. Final Summary

Summarize:

* Database changes
* Backend changes
* API changes
* Frontend changes
* Configuration changes
* Migration or deployment considerations

---

# 36. General Code Quality Rules

Generate production-oriented and maintainable code.

Follow these rules:

* Prefer readable code over clever code.
* Use clear and domain-specific names.
* Keep methods focused.
* Avoid duplicated logic.
* Avoid unnecessary abstractions.
* Follow existing project conventions.
* Do not add dependencies without justification.
* Do not expose sensitive information.
* Do not bypass validation.
* Do not bypass Spring Security.
* Do not duplicate Oracle business logic in Java.
* Do not place database logic in Controllers.
* Do not place UI logic in API modules.
* Do not use `any` to suppress TypeScript errors.
* Do not modify unrelated files.
* Do not perform large refactoring unless explicitly requested.
* Preserve backward compatibility unless a breaking change is approved.

---

# 37. Important Architecture Summary

The most important rules of this project are:

```text
Oracle Database:
Owns business rules, calculations, validations, and data workflows.

Spring Boot:
Provides REST APIs, security, application orchestration, validation,
transaction boundaries, email integration, and database integration.

Repository:
Calls Oracle Package procedures through JdbcTemplate or SimpleJdbcCall,
and queries Views or Functions through JdbcTemplate or NamedParameterJdbcTemplate.

Vue 3 + TypeScript:
Implements the user interface based on the finalized backend API contract.

Development Order:
Oracle → Spring Boot Backend → API Verification → Vue TypeScript Frontend.
```
