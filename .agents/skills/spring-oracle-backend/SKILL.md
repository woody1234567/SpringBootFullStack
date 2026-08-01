---
name: spring-oracle-backend
description: Shared Spring Boot backend conventions for this Oracle-centric project. Use this skill for cross-layer backend work, package structure, DTO rules, API response formats, exception handling, logging, dependency injection, or changes that span Controller, Service, and Repository layers. For layer-specific work, also use spring-oracle-controller, spring-oracle-service, or spring-oracle-repository.
---

# Spring Oracle Backend

## Layer Routing

Keep backend responsibilities separated:

```text
Controller → Service → Repository → Oracle Package / Procedure / Function
```

Use the dedicated layer skills for detailed implementation rules:

- `spring-oracle-controller`: REST controllers, request binding, validation entry points, response DTOs, and HTTP boundary behavior.
- `spring-oracle-service`: application orchestration, authorization checks, Spring-owned transactions, Oracle result-code interpretation, and response DTO conversion.
- `spring-oracle-repository`: Oracle package/procedure/function calls, `JdbcTemplate`, `NamedParameterJdbcTemplate`, `SimpleJdbcCall`, cursor mapping, Oracle type binding, and repository result models.

When a task spans multiple backend layers, use this shared skill plus each relevant layer skill. Do not duplicate layer-specific details here; keep this file focused on shared backend conventions.

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
