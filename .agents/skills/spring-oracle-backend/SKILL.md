---
name: spring-oracle-backend
description: Spring Boot backend conventions for this Oracle-centric project. Use this skill whenever adding or modifying Controllers, Services, Repositories, DTOs, API response formats, exception handling, logging, dependency injection, or Repository calls through JdbcTemplate, NamedParameterJdbcTemplate, or SimpleJdbcCall.
---

# Spring Oracle Backend

## Layer Responsibilities

Keep backend responsibilities separated:

```text
Controller → Service → Repository → Oracle Package / Procedure / Function / View
```

Controllers receive HTTP requests, map parameters, validate request DTOs, call Services, return standardized API responses, and map HTTP status codes.

Controllers must not contain SQL, `JdbcTemplate`, `SimpleJdbcCall`, business calculations, Oracle-specific parameter handling, complex permission logic, or transactional database workflows.

Services are thin application orchestration layers. They coordinate Repository calls, manage application-level transactions, apply Spring Security authorization, retrieve authenticated user information, convert Repository results into response DTOs, call mail or external services, translate database errors, and manage API-level workflows.

Services should not duplicate Oracle business logic. Use `@Transactional` only when multiple Repository calls need a Java-managed transaction or when the application layer owns the transaction boundary.

Repositories are responsible for database communication. They may call Oracle procedures/functions, execute SQL queries, map result sets, register Oracle parameters including `SYS_REFCURSOR`, convert database results into repository result objects, and translate low-level database exceptions when appropriate.

Repositories must not contain business logic. Method names should reflect database operations clearly, such as `createUser`, `verifyEmail`, `resetPassword`, `getUserDetail`, or `checkEmailExists`. Avoid generic names like `execute`, `run`, `call`, or `process` except for internal reusable helpers.

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
mail
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

Use Jakarta Validation annotations for request DTOs. Use Lombok correctly; the annotation is `@Data`, not `@Date`. Prefer more specific Lombok annotations over `@Data` for security-sensitive or immutable classes.

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

Do not log plaintext passwords, password hashes, complete JWT tokens, session IDs, email verification tokens, password reset tokens, confidential personal information, or sensitive database connection information.

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
