# 檔案上傳流程（CSV 批次匯入）

以「CSV 批次匯入費用」功能為例，說明前端檔案上傳的完整流程與各層職責。

## 相關檔案

```
components/import/CsvUploadPanel.vue   # UI 層：選檔、副檔名檢查
views/expense/ExpenseImportView.vue    # 頁面層：流程協調、loading/error 狀態
api/importApi.ts                       # API 層：組裝 FormData、呼叫後端
api/httpClient.ts                      # 共用 Axios 實例（JWT、401 攔截）
types/api/import.ts                    # 匯入結果的回應型別
components/import/ImportResultTable.vue # 顯示匯入結果 / 逐列錯誤
```

## 流程總覽

```
CsvUploadPanel.vue (UI / 選檔)
    ↓ emit('upload', file)
ExpenseImportView.vue (頁面 / 協調)
    ↓ importApi.importExpenses(file)
importApi.ts (API layer)
    ↓ httpClient.post(multipart/form-data)
httpClient.ts (Axios instance)
    ↓
後端 POST /api/imports/expenses
```

## 各層細節

### 1. `CsvUploadPanel.vue` — 純 UI 元件

使用 View UI Plus 的 `<Upload>` 元件，但不讓它自己發請求：

```vue
<Upload action="" :before-upload="handleBeforeUpload" :show-upload-list="false" accept=".csv">
  <Button icon="ios-cloud-upload-outline" :loading="uploading">選擇 CSV 檔案上傳</Button>
</Upload>
```

- `action=""` 讓 `<Upload>` 沒有預設上傳目標。
- `:before-upload="handleBeforeUpload"` 是關鍵：View UI Plus 的 `Upload` 規則是 `before-upload` 回傳 `false` 時會**中止元件內建的自動上傳行為**，改由外部自行處理檔案。
- `handleBeforeUpload` 只做前端層級的早期防護（副檔名必須是 `.csv`），失敗時用 `Message.error` 提示並回傳 `false` 中止；成功則 `emit('upload', file)` 把原生 `File` 物件往上丟給父層，元件本身**不呼叫任何 API**，也不做 CSV 內容驗證（內容驗證屬於後端 / 資料庫層職責）。
- 透過 `defineExpose({ setUploading })` 讓父層可以控制上傳按鈕的 `loading` 狀態。

```ts
const handleBeforeUpload = (file: File) => {
  if (!file.name.toLowerCase().endsWith('.csv')) {
    Message.error('只接受 .csv 檔案')
    return false
  }
  emit('upload', file)
  return false
}
```

### 2. `ExpenseImportView.vue` — 頁面層

接收 `CsvUploadPanel` 的 `@upload` 事件，負責整個上傳流程的協調與狀態呈現：

```ts
const handleUpload = async (file: File) => {
  uploading.value = true
  result.value = null
  try {
    const response = await importApi.importExpenses(file)
    result.value = response.data
  } catch (error) {
    Message.error(getErrorMessage(error))
  } finally {
    uploading.value = false
  }
}
```

- 負責 loading 狀態（`uploading` → `<Spin fix />` 與按鈕 loading）。
- 成功時把回應資料交給 `ImportResultTable` 顯示（含逐列失敗訊息 `failedRows`）。
- 失敗時透過 `useApiError()` 的 `getErrorMessage` 將錯誤正規化後用 `Message.error` 顯示，不會吞掉錯誤。
- 符合專案慣例：頁面層負責 loading / empty / error 狀態，元件與 API 層不處理 UI 提示。

### 3. `importApi.ts` — API 層

把原生 `File` 包裝成 `FormData` 後送出：

```ts
export const importExpenses = (file: File): Promise<ApiResponse<ImportResultData>> => {
  const formData = new FormData()
  formData.append('file', file)

  return httpClient.post('/api/imports/expenses', formData).then((res) => res.data)
}
```

- 刻意**不手動設定** `Content-Type`，交由瀏覽器 / Axios 自動判斷為 `multipart/form-data` 並附上正確的 boundary（手動設定反而容易漏掉 boundary 導致後端解析失敗）。
- API 層只負責端點定義與型別，不含任何 UI 邏輯（如 Message、Modal）。

### 4. `httpClient.ts` — 共用 Axios 實例

上傳請求與其他 API 請求共用同一個 Axios 實例與攔截器：

- Request 攔截器：若 `authStore.token` 存在，自動附加 `Authorization: Bearer <token>`。
- Response 攔截器：收到 `401` 時自動登出並導回登入頁。

### 5. 回應型別 `types/api/import.ts`

```ts
export interface ImportRowError {
  rowNumber: number
  message: string
}

export interface ImportResultData {
  batchId: number
  totalRows: number
  successCount: number
  status: 'SUCCESS' | 'FAILED'
  failedRows: ImportRowError[]
}
```

後端以「整批交易」的方式處理匯入（任一列驗證失敗，整批都不會寫入），前端只需將 `ImportResultData` 原樣呈現給 `ImportResultTable`，不做二次計算。

## 小結

| 層級 | 職責 | 不做的事 |
|---|---|---|
| `CsvUploadPanel.vue` | 選檔、副檔名早期檢查、UI 呈現 | 不呼叫 API、不做內容驗證 |
| `ExpenseImportView.vue` | 流程協調、loading / error / 結果狀態 | 不組 FormData、不知道 API 路徑 |
| `importApi.ts` | 組裝 `FormData`、定義端點與型別 | 不觸發 Message / Modal 等 UI 行為 |
| `httpClient.ts` | 附加 JWT、統一處理 401 | 不含任何業務邏輯 |

這個分層與 `.claude/CLAUDE.md` 中「Frontend API Layer 不應包含 UI 狀態 / 複雜業務邏輯」「頁面負責 loading / empty / error 狀態」的規範一致。

另外，同一個面板上還有一顆「下載範例檔」按鈕，其實作與檔案上傳無關（純前端產生，不經後端 API），詳見 [Download.md](./Download.md)。
