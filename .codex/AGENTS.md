# Project Context for Coding Agents

You are working on an enterprise full-stack web application.

Before modifying code, inspect the existing project structure, coding conventions, database integration patterns, and API response formats. Do not introduce a new framework, ORM, architectural pattern, or dependency unless explicitly requested.

## Core Architecture

This project follows a database-centric architecture:

```text
Vue 3 + TypeScript Frontend
        ↓
Spring Boot REST API
        ↓
Service Layer
        ↓
Repository Layer
        ↓
Oracle Schema / Package / Procedure / Function
        ↓
Oracle Tables
```

Business rules should primarily live in Oracle Database. Spring Boot provides REST APIs, security, application orchestration, validation, transaction boundaries, and database integration. Vue 3 + TypeScript implements the UI based on the finalized backend API contract.

## Development Order

For full-stack work, follow this order:

1. Analyze the business requirement.
2. Design or modify Oracle database objects.
3. Implement business logic in PL/SQL packages, stored procedures, and functions when justified.
4. Implement Spring Boot Repository integration.
5. Implement Spring Boot Service and REST API.
6. Test and confirm the backend API contract.
7. Implement frontend TypeScript types and API client.
8. Implement the Vue frontend interface based on the completed API.
9. Perform frontend and backend integration testing.

The backend API must be completed and stabilized before implementing the frontend interface. The frontend must follow the backend API contract; do not invent mock data structures that conflict with real backend responses.

## Technology Stack

- Frontend: Vue 3, TypeScript, View UI Plus, Pinia, Vue Router, Axios or the existing HTTP client, Vite if already used.
- Backend: Java, Spring Boot, Spring Web, Spring Security, Spring JDBC, Spring Validation, Spring AOP, Jakarta APIs, Lombok, Log4j2, Oracle JDBC Driver.
- Database: Oracle Database, PL/SQL, schemas, packages, procedures, functions, tables, constraints, indexes, sequences or identity columns, object and collection types when necessary.

## Project Skills

Use the project skills for detailed rules instead of keeping every rule in this always-on file:

- `oracle-database-business-logic`: Use for Oracle DDL, PL/SQL, packages, stored procedures, functions, table functions, Oracle result conventions, exception handling, transaction ownership, and database-centric business logic.
- `oracle-naming-convention`: Use whenever creating, modifying, renaming, or reviewing any Oracle database object. This includes tables, indexes, constraints, procedures, functions, packages, sequences, triggers, jobs, materialized views, object types, and table types.
- `spring-oracle-backend`: Use for shared Spring Boot backend conventions, package structure, DTO rules, API response formats, dependency injection, logging, and exception handling across backend layers.
- `spring-oracle-controller`: Use for REST Controllers, route mappings, request binding, validation entry points, authenticated principal usage at the HTTP boundary, and controller response behavior.
- `spring-oracle-service`: Use for Services, application workflows, Spring-owned `@Transactional` boundaries, authorization checks, Oracle result-code interpretation, exception mapping, and response DTO conversion.
- `spring-oracle-repository`: Use for Repositories, `JdbcTemplate`, `NamedParameterJdbcTemplate`, `SimpleJdbcCall`, Oracle package/procedure/function calls, `SYS_REFCURSOR` mapping, Oracle type binding, and repository models.
- `spring-security-auth`: Use for authentication, authorization, JWT/JWE/session decisions, Spring Security configuration, role handling, route protection, account status, and token safety.
- `vue-typescript-frontend`: Use for Vue 3, TypeScript, frontend API clients, HTTP client configuration, Pinia, Vue Router, View UI Plus, forms, component design, frontend permission checks, and loading/empty/error states.
- `fullstack-feature-workflow`: Use when implementing a new feature or workflow that may span Oracle, Spring Boot, API contracts, and Vue.
- `project-testing-requirements`: Use when adding, modifying, planning, or reporting tests and verification.

## Always-On Rules

- Keep layer responsibilities clear: Controllers handle HTTP, Services orchestrate application workflows, Repositories communicate with Oracle, and Oracle owns business rules where appropriate.
- Do not duplicate Oracle business logic in Java unless it is required for HTTP validation, security protection, or early input rejection.
- Do not place SQL, `JdbcTemplate`, `SimpleJdbcCall`, business calculations, or transaction workflows in Controllers.
- Do not place UI state, navigation, modal behavior, or toast presentation logic in frontend API modules.
- Do not rely on frontend permission checks for security; backend authorization remains authoritative.
- Do not expose raw Oracle exceptions, `ORA-` messages, stack traces, password hashes, tokens, or sensitive implementation details through API responses or logs.
- Do not use `any` to bypass TypeScript errors.
- Do not modify unrelated files or perform broad refactors unless explicitly requested.
- Preserve backward compatibility unless a breaking change is approved.

## Code Quality

Generate production-oriented, maintainable code. Prefer readable code over clever code, use clear domain-specific names, keep methods focused, avoid duplicated logic, avoid unnecessary abstractions, follow existing project conventions, and add dependencies only with clear justification.

Do not claim code has been tested unless the tests were actually executed. If tests were not run, state that clearly and explain the practical reason.
