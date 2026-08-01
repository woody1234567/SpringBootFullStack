---
name: spring-oracle-service
description: Spring Boot Service layer conventions for this Oracle-centric project. Use this skill whenever adding or modifying Services, application workflows, Spring-owned transactions, declarative @Transactional boundaries, authorization checks, authenticated user lookups, Oracle result-code interpretation, exception mapping, or conversion from repository models to response DTOs.
---

# Spring Oracle Service

## Responsibility

Services are application orchestration layers. They coordinate Repository calls, manage Spring-owned transactions, apply Spring Security authorization, retrieve authenticated user information when needed, convert Repository results into response DTOs, translate database results into application exceptions, and manage API-level workflows.

Keep Oracle business logic in Oracle packages and procedures where appropriate. Use Services to make application flow explicit without duplicating database rules in Java.

## Service Rules

Services should:

- Use `@Service` and constructor injection.
- Keep dependencies explicit with `final` fields and Lombok `@RequiredArgsConstructor`.
- Own all transaction boundaries with declarative `@Transactional`.
- Use `@Transactional(readOnly = true)` for read-only workflows when useful.
- Use regular `@Transactional` for write workflows and multi-step database operations.
- Choose propagation deliberately, such as `REQUIRED` for the main workflow and `REQUIRES_NEW` only for intentionally independent Spring-managed records.
- Call Repository methods using domain-oriented Java values and DTO/model classes, not Oracle parameter maps.
- Apply backend authorization rules before performing protected operations.
- Map Oracle result codes from Repository models into application exceptions or response DTOs.
- Convert repository model classes into API response DTOs before returning to Controllers.

Services should not build SQL, register Oracle parameters, parse `SYS_REFCURSOR`, expose raw Oracle result codes to Controllers, or use `TransactionTemplate`. Use declarative `@Transactional` boundaries for Spring-owned transactions.

## Transaction Ownership

All application transactions are controlled by Spring.

Oracle stored procedures must not issue `COMMIT` or `ROLLBACK`; commit and rollback behavior is controlled by Spring's transaction manager through the JDBC connection. Avoid service designs that depend on database-owned transactions or autonomous transaction behavior.

For workflows that need separate rollback behavior, split the work into public methods on separate Spring beans so AOP can apply the intended `@Transactional` propagation. Do not rely on private helper methods or same-bean self-invocation for transaction boundaries.

## Authorization

Use Spring Security context or existing authentication helpers to retrieve the current user only when the workflow requires it.

Apply authorization in Services instead of Controllers when the decision depends on application state, ownership checks, role checks, or database data. Keep frontend permission checks as UX only; backend authorization remains authoritative.

Use `spring-security-auth` whenever changing login, token/session behavior, role mapping, account status, or route protection.

## Result Mapping

Repositories may return repository model classes that include Oracle result codes, output IDs, cursors mapped to rows, or batch result details. Services should translate those models into application exceptions or response DTOs.

Use clear exception types such as:

```text
BusinessException
ResourceNotFoundException
DuplicateResourceException
UnauthorizedException
ForbiddenException
DatabaseOperationException
ValidationException
```

Do not leak raw Oracle codes, `ORA-` messages, stack traces, or low-level database details into Controller responses.

## Logging

Use Lombok `@Slf4j` and Log4j2.

Log meaningful workflow starts, important business identifiers, authorization denials, Oracle result codes after Repository calls, and unexpected failures. Do not log plaintext passwords, password hashes, complete JWT/JWE tokens, session IDs, confidential personal information, or sensitive database connection information.
