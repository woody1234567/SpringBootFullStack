---
name: spring-oracle-controller
description: Spring Boot REST Controller conventions for this Oracle-centric project. Use this skill whenever adding or modifying controller classes, REST routes, request binding, validation entry points, authenticated principal usage at the HTTP boundary, API response DTO returns, or controller-level error behavior.
---

# Spring Oracle Controller

## Responsibility

Controllers are the HTTP boundary. They receive requests, bind path/query/body parameters, validate request DTOs, call one Service method per use case, and return standardized API responses.

Use controllers to translate HTTP input into application calls. Keep business decisions, authorization rules, Oracle result-code interpretation, and workflow orchestration in Services.

## Controller Rules

Controllers should:

- Use `@RestController`, clear route prefixes, and explicit HTTP method mappings.
- Keep route names resource-oriented and consistent with the existing API style.
- Accept request DTO classes from `dto/request` and return response DTO classes from `dto/response`.
- Use Java DTO classes with Lombok for request and response shapes.
- Use `@Valid` for request body validation.
- Use Spring annotations such as `@PathVariable`, `@RequestParam`, and `@RequestBody` explicitly.
- Retrieve authenticated principal information only when it is needed to call the Service.
- Call one Service method per use case.
- Return the project's standardized API response format.
- Keep HTTP status behavior consistent with `@RestControllerAdvice`.

Controllers must not contain SQL, `JdbcTemplate`, `NamedParameterJdbcTemplate`, `SimpleJdbcCall`, Oracle parameter names, result-set mapping, transaction annotations, business calculations, complex permission logic, or database workflow branching.

## Request DTOs

Use `dto/request` for request payloads:

```text
dto/request/CreateUserRequest.java
dto/request/SearchExpenseRequest.java
```

Apply Jakarta Validation annotations to request DTO fields when input constraints are known:

```java
@Getter
@Setter
public class CreateUserRequest {

    @NotBlank
    private String username;
}
```

Do not expose Oracle parameter maps, database row models, password hashes, unintended tokens, or internal result codes through request DTOs.

## Response DTOs

Use `dto/response` for API response payloads:

```text
dto/response/UserResponse.java
dto/response/ExpenseDetailResponse.java
```

Controllers should return response DTOs prepared by the Service layer. Avoid returning repository model classes directly, even when fields currently match.

## API Response Format

Use the shared backend response wrapper.

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

Do not return raw Oracle exceptions, `ORA-` messages, stack traces, password hashes, tokens, or sensitive implementation details to the frontend.

## Logging

Use Lombok `@Slf4j` only when the controller has meaningful HTTP-boundary events to log. Prefer logging stable identifiers, route-level intent, validation failures, and unexpected exceptions. Do not log plaintext passwords, password hashes, complete JWT/JWE tokens, session IDs, confidential personal information, or request bodies that may contain sensitive values.
