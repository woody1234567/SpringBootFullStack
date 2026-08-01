---
name: spring-security-auth
description: Authentication and authorization rules for this Spring Boot and Oracle project. Use this skill whenever implementing or changing login, JWT/session security, roles, route protection, Spring Security filters, tokens, or account status logic.
---

# Spring Security and Auth

## Security Model

The system may support credential authentication, JWT authentication, JWE authentication, Session authentication, role-based authorization, account status checks, and token validation.

Do not implement multiple login token/session modes simultaneously unless the requirement explicitly calls for it. Before implementing authentication, determine which mode the existing project uses.

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

## Login Token Options

Before implementing or changing login behavior, confirm the login scheme with the user. Present the practical options and wait for the user's choice when the requirement does not already specify one:

- JWT: signed token. Use when the project wants stateless authentication and does not need to encrypt claims inside the token.
- JWE: encrypted token. Use when token claims may contain personal data or sensitive authorization context that should not be readable by the client even if the token is stored or inspected.
- Session: server-side session. Use when the project wants server-managed login state and can accept session storage and CSRF considerations.

For JWE, keep the token encrypted and authenticated, store encryption keys outside source control, rotate keys deliberately, and keep claims minimal even though they are encrypted. JWE protects confidentiality of token contents; it does not replace HTTPS, password hashing, authorization checks, token expiry, or secure storage on the client.

## Oracle Responsibilities

Oracle may handle:

- User lookup
- Account status
- Role lookup
- Failed login counter
- Account locking
- Token record management

## Spring Security Responsibilities

Spring Security handles:

- Authentication context
- Request filters
- Endpoint protection
- Role and authority mapping
- JWT/JWE validation or Session management
- CSRF configuration
- Password encoding
- Security exception responses

Passwords must be hashed using a secure password encoder such as BCrypt or the project's configured encoder. Never store plaintext passwords.

Do not log plaintext passwords, password hashes, complete JWT/JWE tokens, session IDs, one-time tokens, or confidential personal information.
