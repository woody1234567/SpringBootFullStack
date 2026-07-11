# Expense Tracker – Frontend

Vue 3 + TypeScript single-page application for tracking personal expenses. Built with Vite, Pinia, Vue Router, and View UI Plus, and talks to a Spring Boot REST API backed by SQL Server.

## Features

- **Authentication** – Email/password login and registration (`/login`, `/register`). A JWT is stored in `localStorage` and attached to every API request; a 401 response automatically logs the user out and redirects to login.
- **Expense list & management** (`/expenses`) – Paginated table of expense records with filtering by date range and category, plus create/edit/delete via a modal form.
- **CSV batch import** (`/expenses/import`) – Upload a CSV file to bulk-create expenses. The import is all-or-nothing: if any row fails validation, none of the batch is committed. Per-row results (success/failure with reasons) are shown after upload.
- **Route guards** – Authenticated routes redirect to `/login` when no session exists; visiting `/login` or `/register` while already authenticated redirects to `/expenses`. Unknown paths render a 404 view.

## Project Structure

```text
src
├── api            # Axios-based API client functions (auth, category, expense, import)
├── components
│   ├── expense     # Expense filter bar, table, and create/edit modal
│   ├── import      # CSV upload panel and import result table
│   └── layout      # App header/navigation
├── composables     # useAuth, useApiError, useExpenseFilters, usePagination
├── router          # Route definitions and auth guards
├── stores          # Pinia auth store (session/token persistence)
├── types
│   ├── api         # Request/response DTOs matching the backend contract
│   ├── domain      # Domain models (Expense, Category, User)
│   └── form        # Form-only state shapes (login, register, expense form)
├── utils           # Date formatting, file download helpers
└── views
    ├── auth        # Login / Register pages
    └── expense     # Expense list and CSV import pages
```

## Backend Integration

The app expects a Spring Boot API reachable at `VITE_API_BASE_URL` (see `.env.example`, defaults to `http://localhost:8080`). All responses follow the shape:

```json
{
  "success": true,
  "message": "...",
  "data": {}
}
```

Requests are authenticated with a `Bearer` token attached automatically by the shared `httpClient` (`src/api/httpClient.ts`).

## Development

```sh
npm install
npm run dev       # start the Vite dev server
npm run build     # type-check (vue-tsc) and build for production
npm run preview   # preview the production build locally
```

Copy `.env.example` to `.env` and adjust `VITE_API_BASE_URL` to point at your backend before running the dev server.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue - Official](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (disable Vetur if installed).
