---
name: project-testing-requirements
description: Testing expectations for this Oracle + Spring Boot + Vue project. Use this skill whenever adding or modifying tests, planning verification, reporting test coverage, or finishing backend, frontend, database, security, or integration changes.
---

# Project Testing Requirements

## Backend Testing

For backend implementation, consider:

- Unit tests for Java mapping and application logic
- Integration tests for Repository calls
- API tests for Controllers
- Security tests
- Oracle stored procedure and PL/SQL tests
- Transaction rollback tests
- Validation tests

Scale backend tests with risk. Broaden coverage when changing shared API contracts, security behavior, transaction ownership, database integration, or exception mapping.

## Frontend Testing

For frontend implementation, consider:

- Type checking
- Component tests
- Store tests
- API error handling
- Route guard tests
- Form validation tests
- Loading and empty state tests

Verify that frontend types match the finalized backend API contract.

## Reporting

Do not claim that code has been tested unless the tests were actually executed.

When tests were not run, say so and give the practical reason. When only partial verification was possible, state what was covered and what remains.
