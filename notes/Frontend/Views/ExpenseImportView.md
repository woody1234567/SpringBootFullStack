# ExpenseImportView.vue 元件溝通說明

`ExpenseImportView.vue` 是消費紀錄 CSV 批次匯入頁面的「容器/協調者」，持有 `uploading`、`result` 兩個狀態，並透過 **emit 事件（子→父傳動作）** 和 **props（父→子傳資料）** 跟兩個子元件溝通，是標準的 Vue 3 單向資料流。

## 1. CsvUploadPanel（上傳面板）

```html
<CsvUploadPanel @upload="handleUpload" />
```

- **不吃 props**：`CsvUploadPanel` 是完全自足的展示型元件，內含檔案類型檢查、CSV 欄位規格說明（`Alert`）、以及下載範例檔案（`downloadTextFile`）的功能，都不需要父層提供資料。
- **`upload` 事件**：使用者透過 `Upload` 選檔後，子元件在 `handleBeforeUpload` 檢查副檔名是否為 `.csv`；不符合就用 `Message.error` 提示並 `return false` 擋下。符合的話 emit `upload(file)` 把原始 `File` 物件丟給父層，同樣 `return false` 阻止 View UI Plus 的 `Upload` 元件自動送出請求（實際上傳交給父層處理）。
- 父層 `handleUpload` 收到檔案後，設定 `uploading.value = true`、清空舊的 `result`，呼叫 `importApi.importExpenses(file)`，成功則寫入 `result.value = response.data`，失敗則用 `useApiError().getErrorMessage(error)` 正規化錯誤訊息並用 `Message.error` 顯示，最後 `finally` 把 `uploading.value = false`。
- 子元件本身不持有上傳中狀態，上傳中的視覺回饋完全由父層的 `<Spin v-if="uploading" fix />` 蓋住整個 `Card` 呈現。

## 2. ImportResultTable（匯入結果表格）

```html
<ImportResultTable v-if="result" :result="result" />
```

- **純 props 傳入**：`result`（型別 `ImportResultData`），只在父層 `result` 不為 `null` 時才渲染。
- `ImportResultTable` 完全是展示型元件，沒有 emit、沒有自己的 API 呼叫。依 `result.status` 顯示成功或失敗的 `Alert`；若 `result.failedRows` 有資料，額外渲染錯誤列表（列號 + 錯誤原因）。
- 這裡呼應頁面上方的提示文字「整批匯入為單一交易：任何一筆資料驗證失敗，整批都不會寫入」——只要 `status !== 'SUCCESS'`，就代表整批都沒寫入，`successCount` 恆為 0。

## 3. AppHeader（頁首）

```html
<AppHeader />
```

- 沒有 props、沒有 emit，純版面配置，不參與資料溝通。

## 4. 非元件的溝通（API 層與 composable）

- `importApi.importExpenses(file)`：呼叫後端匯入 API，回傳值寫入 `result`。
- `useApiError().getErrorMessage(error)`：把 catch 到的 error 正規化成使用者可讀的字串，僅用於 `Message.error`，不牽涉元件間通訊。

## 整體資料流總結

- **狀態集中在父層**：`uploading`、`result` 全部由 `ExpenseImportView` 持有，兩個子元件都不持有跨渲染週期的業務狀態。
- **子元件不打 API**：`importApi` 呼叫集中在父層，`CsvUploadPanel` 只負責選檔/驗證副檔名並 emit 事件，`ImportResultTable` 只負責依 props 渲染結果，符合專案慣例中「API 呼叫集中在 API 層，UI 元件不直接處理業務邏輯」的原則。
- **典型循環**：使用者選擇 CSV 檔案 → `CsvUploadPanel` emit `upload(file)` → 父層 `handleUpload` 呼叫 API 並更新 `uploading` / `result` → `ImportResultTable` 因 `result` 改變而顯示 / 重新渲染。

```text
CsvUploadPanel --emit upload(file)--> ExpenseImportView --props result--> ImportResultTable
                                             │
                                             ├─ importApi.importExpenses(file)
                                             └─ useApiError.getErrorMessage(error)
```
