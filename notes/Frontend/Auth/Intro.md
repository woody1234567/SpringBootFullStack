# 前端 Auth 流程

採用 JWT-based 認證（非 Session-based）。相關檔案：

- `frontend/src/stores/authStore.ts`
- `frontend/src/composables/useAuth.ts`
- `frontend/src/api/authApi.ts`
- `frontend/src/api/httpClient.ts`
- `frontend/src/router/index.ts`
- `frontend/src/types/api/auth.ts`
- `frontend/src/views/auth/LoginView.vue`

## 1. 登入 / 註冊

`LoginView.vue` → `useAuth()` → `authStore`

- 表單提交呼叫 `useAuth().login()`，即 `authStore.login()`（`authStore.ts:25`）
- `authStore` 呼叫 `authApi.login()`（`authApi.ts:9`），POST 到後端 `/api/auth/login`
- 拿到 `{ token, user }` 後呼叫 `setSession()`（`authStore.ts:42`），同時存進：
  - Pinia state（記憶體）
  - `localStorage`（key: `expense_tracker_token` / `expense_tracker_user`）

存進 `localStorage` 是為了讓使用者刷新頁面後仍保持登入狀態。

## 2. Token 附加到請求

`httpClient.ts:10-16`

Axios request interceptor 在每次請求前讀取 `authStore.token`，自動加上：

```
Authorization: Bearer <token>
```

## 3. 401 自動登出

`httpClient.ts:18-28`

Response interceptor 攔截到後端回傳 401 時：

1. 呼叫 `authStore.logout()`，清空 Pinia state + `localStorage`
2. 導回 `/login`

## 4. 路由守衛

`router/index.ts:49-61`

`beforeEach` 檢查 `to.meta.requiresAuth`：

- 需要登入但 `authStore.isAuthenticated` 為 `false`（token 或 user 任一為 null，見 `authStore.ts:21`）→ 導向 `login`，並帶上 `redirect` query 記住原本要去的頁面。
- 已登入卻想進 `login` / `register` → 直接導向 `expenses`。

路由 meta 設定：

| 路由 | requiresAuth |
|---|---|
| `/login` | `false` |
| `/register` | `false` |
| `/expenses` | `true` |
| `/expenses/import` | `true` |

## 注意事項

- 路由守衛只是前端 UX 層的保護，實際安全邊界仍在後端 Spring Security 驗證 JWT（符合專案 CLAUDE.md 第 27 節：前端權限檢查僅供使用者體驗，後端必須獨立強制授權）。
- Token 目前以明文存在 `localStorage`（非 httpOnly cookie），是常見的 XSS 風險點。若之後要改成更安全的儲存方式（例如 httpOnly cookie + CSRF token），需要同步調整後端 auth 流程與 CORS/CSRF 設定。
