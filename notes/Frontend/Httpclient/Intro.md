# 前端 Axios HttpClient

相關檔案：

- `frontend/src/api/httpClient.ts`
- `frontend/src/api/authApi.ts`
- `frontend/src/api/categoryApi.ts`
- `frontend/src/api/expenseApi.ts`
- `frontend/src/api/importApi.ts`
- `frontend/src/stores/authStore.ts`
- `frontend/src/router/index.ts`

## 1. 建立共用實例

`httpClient.ts:5-8`

```ts
const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  timeout: 15000,
})
```

用 `axios.create()` 產生獨立配置的實例，而不是用全域的 `axios` 物件：

- `baseURL` 統一指向後端 Spring Boot（開發環境沒設環境變數時 fallback 到 `localhost:8080`）
- 所有請求統一套用 15 秒 timeout
- 之後掛的 interceptor 只影響這個實例，不會污染全域 axios

## 2. Request Interceptor —— 自動夾帶 JWT

`httpClient.ts:10-16`

```ts
httpClient.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})
```

每次呼叫 `httpClient.get/post/...` 送出前都會先經過這段。讀取 Pinia 的 `authStore.token`，有登入 token 就自動加上 `Authorization: Bearer <token>` header，讓所有業務 API 模組都不用自己處理帶 token 這件事。

## 3. Response Interceptor —— 統一處理 401

`httpClient.ts:18-28`

```ts
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push({ name: 'login' })
    }
    return Promise.reject(error)
  },
)
```

成功的 response 直接原樣放行；失敗時攔截檢查，若後端回 401（token 過期或未授權），自動呼叫 `authStore.logout()` 並導回登入頁，接著把 error 繼續往下 reject，讓呼叫端仍能拿到錯誤去顯示訊息。這是一個全域的認證失效處理點，不需要每個頁面自行判斷 401。

## 4. 上層 API 模組如何使用它

`src/api/` 底下的 `authApi.ts`、`categoryApi.ts`、`expenseApi.ts`、`importApi.ts` 都 import 這個共用實例來發請求，例如 `importApi.ts:1-10`：

```ts
import httpClient from '@/api/httpClient'

export const importExpenses = (file: File): Promise<ApiResponse<ImportResultData>> => {
  const formData = new FormData()
  formData.append('file', file)
  return httpClient.post('/api/imports/expenses', formData).then((res) => res.data)
}
```

符合 CLAUDE.md 第 20 節「API layer」規範：元件不會直接 import axios，而是呼叫這些具名、有型別的函式（回傳 `Promise<ApiResponse<T>>`）。HTTP 細節（baseURL、header、401 處理）都封裝在 `httpClient` 這一層，元件只需要 `.then/.catch` 拿資料或錯誤。

## 完整請求流程

```
Vue 元件 / composable
    ↓ 呼叫
api/xxxApi.ts (例如 importExpenses)
    ↓ 呼叫
httpClient (axios instance)
    ↓ request interceptor 自動加 Authorization
    ↓ 送出 HTTP 請求到後端
    ↓ response interceptor 檢查是否 401
    ↓ 回傳 res.data (unwrap AxiosResponse)
api 函式回傳 Promise<ApiResponse<T>>
    ↓
元件拿到強型別資料，處理 loading/empty/error 狀態
```

## 注意事項

- 「附加 token」與「401 自動登出」這兩個橫切關注點只在 `httpClient.ts` 寫一次，其餘 API 模組重複利用，符合單一職責與 DRY 原則。
- 與 [[Auth/Intro]] 的認證流程共用同一支 `httpClient.ts`，兩份筆記描述的是同一段程式碼的不同面向（Auth 筆記聚焦登入/登出流程，本篇聚焦 Axios 實例本身的配置與攔截器機制）。
