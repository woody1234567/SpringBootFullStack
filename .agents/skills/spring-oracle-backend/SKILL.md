---
name: spring-oracle-backend
description: Spring Boot backend conventions for this Oracle-centric project. Use this skill whenever adding or modifying Controllers, Services, Repositories, DTOs, API response formats, exception handling, logging, dependency injection, or Repository calls through JdbcTemplate, NamedParameterJdbcTemplate, or SimpleJdbcCall.
---

# Spring Oracle Backend

## Layer Responsibilities

Keep backend responsibilities separated:

```text
Controller → Service → Repository → Oracle Package / Procedure / Function
```

### Controller Development

Controllers are the HTTP boundary. They receive requests, bind path/query/body parameters, validate request DTOs, call one Service method per use case, and return standardized API responses.

Controllers should:

- Use `@RestController`, clear route prefixes, and explicit HTTP method mappings.
- Accept request DTO classes from `dto/request` and return response DTO classes from `dto/response`.
- Use `@Valid` for request body validation and simple Spring annotations for path/query parameters.
- Retrieve authenticated principal information only when it is needed to call the Service.
- Keep HTTP status behavior consistent with the API response format and exception handling policy.
- Delegate business decisions, authorization rules, Oracle result-code interpretation, and workflow orchestration to Services.

Controllers must not contain SQL, `JdbcTemplate`, `SimpleJdbcCall`, Oracle parameter names, result-set mapping, transaction annotations, business calculations, complex permission logic, or database workflow branching.

### Service Development

Services are thin application orchestration layers. They coordinate Repository calls, manage Spring-owned transactions, apply Spring Security authorization, retrieve authenticated user information when needed, convert Repository results into response DTOs, translate database results into application exceptions, and manage API-level workflows.

Services should:

- Use constructor injection and keep dependencies explicit.
- Own all transaction boundaries with declarative `@Transactional`.
- Use `@Transactional(readOnly = true)` for read-only workflows when useful.
- Use regular `@Transactional` for write workflows and multi-step database operations.
- Choose propagation deliberately, such as `REQUIRED` for the main workflow and `REQUIRES_NEW` only for intentionally independent Spring-managed records.
- Call Repository methods using domain-oriented Java values and DTO/model classes, not Oracle parameter maps.
- Map Oracle result codes from Repository models into application exceptions or response DTOs.
- Convert repository model classes into API response DTOs before returning to Controllers.

Services should not duplicate Oracle business logic already implemented in procedures or functions. They should not build SQL, register Oracle parameters, parse `SYS_REFCURSOR`, expose raw Oracle result codes to Controllers, or use `TransactionTemplate` unless explicitly requested. Oracle stored procedures must not issue `COMMIT` or `ROLLBACK`; all commit and rollback behavior is controlled by Spring.

### Repository Development

Repositories are the Oracle integration boundary. They call Oracle procedures/functions, execute SQL queries when appropriate, register Oracle parameters including `SYS_REFCURSOR`, bind SQL object/table types, map result sets, convert database outputs into repository result model classes, and translate low-level database exceptions when appropriate.

Repositories should:

- Provide an interface plus an implementation under `repository/impl` when matching the existing style.
- Use `SimpleJdbcCall` for stored procedures and packaged procedures when it fits the project pattern.
- Use `JdbcTemplate` or `NamedParameterJdbcTemplate` for focused SQL calls, scalar function calls, and simple lookups.
- Declare Oracle parameters explicitly when metadata lookup is unreliable or costly.
- Keep parameter names aligned with PL/SQL names, including `I_...` input parameters and `O_...` output parameters.
- Use named constants for reused Oracle parameter names and result keys.
- Map `SYS_REFCURSOR` rows through dedicated row mappers under `repository/mapper`.
- Return typed repository model classes from `repository/model`, not raw JDBC or Oracle driver objects.

Repositories must not contain business rules, HTTP status mapping, Spring Security authorization, Service workflow orchestration, or transaction ownership decisions. Method names should reflect database operations clearly, such as `createExpense`, `updateExpense`, `getExpenseDetail`, `searchExpenses`, or `categoryExists`. Avoid generic names like `execute`, `run`, `call`, or `process` except for internal reusable helpers.

## Package Structure

Follow the existing root package. Use a structure similar to:

```text
config
controller
dto/request
dto/response
exception
repository/model
repository/mapper
repository/impl
security
service
aspect
constant
util
```

Do not reorganize the entire project unless explicitly requested.

## DTO Rules

Use separate DTOs for API requests and responses:

```text
dto/request/CreateUserRequest.java
dto/response/UserResponse.java
```

Do not expose Oracle parameter maps, raw JDBC result-set structures, database row objects, password hashes, unintended security tokens, or internal database result codes unless they are part of the documented API.

Create DTOs as Java classes and use Lombok for boilerplate. Use Jakarta Validation annotations for request DTOs. Use Lombok correctly; the annotation is `@Data`, not `@Date`. Prefer more specific Lombok annotations over `@Data` for security-sensitive or immutable classes.

## Repository Model Rules

Create repository result models as Java classes under `repository/model` and use Lombok for getters, constructors, and builders when useful.

Do not pass raw `Map<String, Object>`, `SqlParameterSource`, JDBC `ResultSet`, Oracle `STRUCT`, Oracle `ARRAY`, or cursor objects across the Repository boundary. Repositories should convert database outputs into typed repository model classes before returning to Services.

## Dependency Injection

Prefer constructor injection:

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
}
```

Avoid field injection with `@Autowired`.

## Logging

Use Lombok `@Slf4j` and Log4j2.

Log useful operation starts, important business identifiers, Oracle package/procedure/function names, result codes, execution failures, unexpected exceptions, and security-sensitive events.

Do not log plaintext passwords, password hashes, complete JWT tokens, session IDs, one-time tokens, confidential personal information, or sensitive database connection information.

## Exception Handling

Use `@RestControllerAdvice`.

Define clear application exceptions when appropriate:

```text
BusinessException
ResourceNotFoundException
DuplicateResourceException
UnauthorizedException
ForbiddenException
DatabaseOperationException
ValidationException
```

Map exceptions to appropriate HTTP status codes:

```text
Validation error      → 400 Bad Request
Authentication error  → 401 Unauthorized
Authorization error   → 403 Forbidden
Not found             → 404 Not Found
Duplicate resource    → 409 Conflict
Unexpected error      → 500 Internal Server Error
```

## API Response Format

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

Do not return raw Oracle exceptions or `ORA-` error messages to the frontend. Keep response formats consistent across all controllers.
