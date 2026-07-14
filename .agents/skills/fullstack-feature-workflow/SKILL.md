---
name: fullstack-feature-workflow
description: Backend-first full-stack feature workflow for this Oracle + Spring Boot + Vue project. Use this skill whenever the user asks to implement a new feature, full workflow, CRUD flow, authenticated feature, or frontend/backend integration that may involve database, API, and Vue changes.
---

# Fullstack Feature Workflow

## Development Order

Implement full-stack features in this order:

```text
Requirement analysis
→ Oracle design and business logic
→ Spring Boot Repository integration
→ Spring Boot Service and REST API
→ API contract verification
→ Frontend TypeScript types and API client
→ Vue interface
→ Integration testing
```

The backend API must be completed and stabilized before implementing the frontend interface. The frontend follows the backend API contract; do not design mock frontend data structures that conflict with actual backend responses.

## Before Implementation

Before implementing a feature:

1. Inspect relevant files.
2. Identify existing coding conventions.
3. Identify related Oracle objects.
4. Determine whether similar functionality already exists.
5. Explain the implementation plan when the change is substantial.
6. List files and database objects to create or modify when useful.
7. Implement Oracle business logic first.
8. Implement Spring Boot backend.
9. Verify the API contract.
10. Implement Vue TypeScript frontend.
11. Run or describe relevant tests.
12. Summarize changes and remaining risks.

Do not start with frontend implementation when the backend API does not yet exist. Do not create mock endpoints as a substitute for the requested backend implementation unless explicitly requested.

## Feature Phases

Requirement analysis identifies the business goal, user role, input data, output data, business rules, validation rules, database changes, security requirements, and email or notification requirements.

Oracle design defines tables or table changes, indexes, constraints, sequences or identity columns, views, package specifications, stored procedure signatures and implementations, procedure inputs and outputs, `SYS_REFCURSOR` parameters, function return values, result set structures, result codes, transaction behavior, and exception behavior.

Backend integration implements Repository methods, `SimpleJdbcCall` configuration, Oracle parameter mapping, result-set mapping, Service orchestration, security checks, DTOs, Controller endpoints, exception mapping, logging, and tests.

API verification confirms request contracts, response contracts, HTTP status, authentication behavior, authorization behavior, validation behavior, Oracle result-code mapping, error responses, and API documentation.

Frontend implementation adds TypeScript request and response types, API client functions, Pinia state when shared state is required, composables when reusable workflows are required, Vue pages, reusable components, View UI Plus validation, loading states, empty states, error states, permission control, and router integration.

Integration testing verifies successful flow, validation failure, unauthorized request, forbidden request, record not found, duplicate data, Oracle exception, network error, repeated submission, expired authentication, and frontend/backend type consistency.

## Feature Response Shape

When asked to describe or implement a feature, organize the response around:

- Requirement understanding and assumptions
- Implementation plan
- Files and Oracle objects
- Oracle implementation
- Spring Boot implementation
- API contract
- Vue TypeScript implementation
- Tests
- Final summary

Keep the amount of detail proportional to the task. For direct implementation work, do the work rather than only describing it.

## Architecture Summary

Oracle owns business rules, calculations, validations, and data workflows.

Spring Boot provides REST APIs, security, application orchestration, validation, transaction boundaries, email integration, and database integration.

Repositories call Oracle package procedures through `JdbcTemplate` or `SimpleJdbcCall`, and query views or functions through `JdbcTemplate` or `NamedParameterJdbcTemplate`.

Vue 3 + TypeScript implements the user interface based on the finalized backend API contract.
