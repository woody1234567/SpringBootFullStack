# Pinia 筆記

本專案前端使用 [Pinia](https://pinia.vuejs.org/) 作為狀態管理工具，搭配 Vue 3 + TypeScript + `<script setup>`。

Store 檔案位置：`frontend/src/stores/`

---

## 目前已存在的 Store

### `authStore.ts`（store id: `auth`）

負責管理使用者登入狀態與 JWT token，並將登入狀態持久化到 `localStorage`，讓重新整理頁面後登入狀態不會遺失。

檔案：`frontend/src/stores/authStore.ts`

#### State

```typescript
interface AuthState {
  user: User | null
  token: string | null
}
```

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `user` | `User \| null` | 目前登入的使用者（`userId`、`email`、`displayName`），初始值從 `localStorage`（key: `expense_tracker_user`）還原 |
| `token` | `string \| null` | JWT token，初始值從 `localStorage`（key: `expense_tracker_token`）還原 |

`User` 型別定義於 `frontend/src/types/domain/user.ts`：

```typescript
export interface User {
  userId: number
  email: string
  displayName: string | null
}
```

#### Getters

| Getter | 說明 |
| --- | --- |
| `isAuthenticated` | 當 `token` 與 `user` 皆不為 `null` 時回傳 `true`，用於判斷是否已登入 |

#### Actions

| Action | 說明 |
| --- | --- |
| `login(payload: LoginRequest)` | 呼叫 `authApi.login`，成功後透過 `setSession` 寫入 state 與 `localStorage` |
| `register(payload: RegisterRequest)` | 呼叫 `authApi.register`，成功後透過 `setSession` 寫入 state 與 `localStorage` |
| `logout()` | 清空 `user`、`token`，並移除 `localStorage` 中對應的 key |
| `setSession(token, user)` | 內部共用方法，同步更新 state 與 `localStorage`（由 `login`、`register` 呼叫） |

#### localStorage keys

| Key | 對應內容 |
| --- | --- |
| `expense_tracker_token` | JWT token（字串） |
| `expense_tracker_user` | 使用者物件（JSON 字串） |

#### 使用範例

```typescript
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()

// 登入
await authStore.login({ email, password })

// 檢查是否登入（例如在 router guard 中）
if (!authStore.isAuthenticated) {
  // 導向登入頁
}

// 登出
authStore.logout()
```

---

## 目前缺少的 Store（尚未實作）

依照專案的功能範圍（記帳應用），以下模組目前 **尚未** 有對應的 Pinia store，資料皆為各 View/Component 的本地 state：

- 支出（Expense）
- 分類（Category）
- 匯入紀錄（Expense Import）

若未來這些資料需要跨頁面共享，可依 `authStore.ts` 的模式（`state` + `getters` + `actions`）新增對應 store，並放在 `frontend/src/stores/` 目錄下。

---

## 慣例與注意事項

- 只有跨頁面 / 跨元件共用的狀態才放進 Pinia，單一元件使用的狀態應留在元件內（見專案 `CLAUDE.md` 第 23 節）。
- Store 內不應直接觸發 View UI Plus 的 Modal / Message 等 UI 邏輯，UI 呈現邏輯應留在元件層。
- Store 的 `state`、`getters`、`actions` 型別都應明確標註，不可使用 `any`。
