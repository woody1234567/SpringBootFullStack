---
name: spring-security-auth-email
description: Authentication, authorization, email verification, and password reset rules for this Spring Boot and Oracle project. Use this skill whenever implementing or changing login, JWT/session security, roles, route protection, Spring Security filters, email verification, password reset, tokens, or account status logic.
---

# Spring Security, Auth, and Email

## Security Model

The system may support email/password authentication, JWT authentication or Session authentication, role-based authorization, email verification, and password reset.

Do not implement JWT and Session authentication simultaneously unless the requirement explicitly calls for both. Before implementing authentication, determine which mode the existing project uses.

Typical authentication flow:

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

## Oracle Responsibilities

Oracle may handle:

- User lookup
- Account status
- Role lookup
- Email verification status
- Failed login counter
- Account locking
- Password reset state
- Token record management

## Spring Security Responsibilities

Spring Security handles:

- Authentication context
- Request filters
- Endpoint protection
- Role and authority mapping
- JWT validation or Session management
- CSRF configuration
- Password encoding
- Security exception responses

Passwords must be hashed using a secure password encoder such as BCrypt or the project's configured encoder. Never store plaintext passwords.

## Email Verification and Password Reset

Use Spring Mail for sending emails.

The database may manage verification token records, reset token records, token expiry time, token usage status, and user verification status.

The backend is responsible for generating secure random tokens, hashing tokens before database storage when appropriate, sending email, validating API requests, calling the related Oracle stored procedure, and returning a safe response.

Do not reveal whether an email account exists during password reset unless the project requirement explicitly permits it.

Do not log plaintext passwords, password hashes, complete JWT tokens, session IDs, email verification tokens, password reset tokens, or confidential personal information.
