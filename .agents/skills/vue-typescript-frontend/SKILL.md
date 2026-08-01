---
name: vue-typescript-frontend
description: Vue 3 + TypeScript frontend conventions for this project. Use this skill whenever adding or modifying .vue components, TypeScript frontend types, API clients, Axios/http clients, Pinia stores, Vue Router routes or guards, View UI Plus forms/tables/modals, frontend permission checks, or loading/empty/error states.
---

# Vue TypeScript Frontend

## Architecture

The frontend uses Vue 3 with TypeScript, View UI Plus, Pinia, Vue Router, Axios or the existing HTTP client, and Vite if already used by the project.

Follow the existing structure. A typical structure is:

```text
src
├── api
├── assets
├── components
│   ├── common
│   ├── form
│   └── layout
├── composables
├── constants
├── layouts
├── router
├── stores
├── types
│   ├── api
│   ├── domain
│   └── form
├── utils
├── views
└── App.vue
```

Feature-based organization may be used for larger modules, but do not restructure the entire frontend without explicit approval.

## API-First Frontend

Frontend implementation begins only after the backend API is available or its contract has been explicitly finalized.

Before implementing a frontend feature, obtain or define the endpoint, method, path parameters, query parameters, request body, response body, error response, authentication requirement, role requirement, pagination behavior, date/time format, and nullable fields.

Create TypeScript interfaces from real backend DTOs. Do not guess field names or invent response properties without updating the backend contract. When backend and frontend fields differ, document and implement an explicit mapping.

## TypeScript Rules

Use TypeScript for all new frontend code and prefer:

```vue
<script setup lang="ts">
```

Avoid `any`, untyped API responses, duplicated interfaces, implicitly shaped objects, direct access to unknown error objects, and large components with mixed responsibilities.

Use `interface` for object contracts, `type` for unions and aliases, generic API response types, typed props, typed emits, typed Pinia state, and typed composable returns.

Use `unknown` with type narrowing when the actual error or value type is not known.

## API Layer and HTTP Client

Place HTTP calls in the API layer. Do not call Axios directly from page components unless the existing project explicitly follows that pattern.

Recommended files:

```text
src/api/httpClient.ts
src/api/authApi.ts
src/api/userApi.ts
```

The API layer owns endpoint definitions, request and response typing, query parameter serialization, HTTP client invocation, and basic response normalization. It should not contain UI state, navigation, modal behavior, toast presentation logic, or complex business logic.

Create a shared HTTP client for base URL, authorization headers, timeout, credentials, JWT/JWE attachment, standard error conversion, unauthorized handling, and correlation IDs when required.

For Session authentication, use `withCredentials: true` when required. For JWT or JWE authentication, attach the token according to the project's security design. Do not implement token storage before confirming the backend authentication strategy.

## Type Organization

Separate API types, domain types, and form types:

```text
types/api/auth.ts
types/api/user.ts
types/domain/user.ts
types/form/loginForm.ts
```

Form state may differ from API DTOs. Do not force form-only fields into backend API DTOs.

## Pinia

Use Pinia for shared application state such as authenticated user, authentication status, roles and permissions, global preferences, shared reference data, and data used across unrelated views.

Do not place every page variable in Pinia. Keep local component state local when it is only used by one component or view.

Use typed state, getters, and actions. Pinia actions may coordinate API calls and shared state, but should not contain View UI Plus modal code or direct DOM operations.

## Components and Composables

Page components handle page-level layout, composables or stores, child component coordination, route parameters, and loading/empty/error states.

Reusable components render UI, receive typed props, emit typed events, and avoid direct API calls when possible.

Extract reusable UI components, composables, type definitions, API calls, or utilities when a `.vue` file becomes difficult to understand.

Use composables for reusable stateful frontend logic such as loading state, error state, API workflows, pagination, route-related behavior, and form submission. Stateless helpers belong in `utils`.

## View UI Plus

Use View UI Plus as the primary UI component library. Prefer existing components before custom replacements:

```text
Form, FormItem, Input, Select, Button, Table, Modal, Message, Notice,
Pagination, DatePicker, Checkbox, Radio, Dropdown, Spin, Alert
```

Keep presentation behavior in Vue components. Do not trigger View UI Plus notifications from frontend API modules.

Frontend validation improves user experience, but it does not replace backend validation or Oracle business validation.

## Permissions and Router

Frontend permission checks are for user experience only. The backend must always enforce authorization independently.

Use frontend permission utilities or composables to hide menu items, disable actions, prevent navigation to restricted pages, and display role-specific content.

Use route metadata for authentication and authorization requirements:

```typescript
{
  path: '/admin/users',
  component: () => import('@/views/admin/UserListView.vue'),
  meta: {
    requiresAuth: true,
    roles: ['ADMIN']
  }
}
```

Router guards may check authentication initialization, redirect unauthenticated users, check role requirements, and prevent access to restricted routes. Vue Router is not a security boundary; Spring Security is.

## Forms and Page States

Forms should include typed form state, View UI Plus validation, loading state, disabled state during submission, backend validation error display, safe error normalization, success feedback, and duplicate submission prevention.

Every data-driven page should explicitly handle initial loading, refresh loading, empty result, API error, permission denied, and successful result. Do not display an empty table without explaining whether data is loading, unavailable, or genuinely empty.
