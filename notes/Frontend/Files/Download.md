# 檔案下載：範例 CSV 檔下載

以「CSV 批次匯入」面板上的「下載範例檔」按鈕為例，說明前端「純前端檔案下載」的實作方式。

**重點：這個下載流程完全不經過後端 API**，是純前端在瀏覽器記憶體中組出檔案內容並觸發下載，跟上方的[檔案上傳流程](./Upload.md)是兩條完全獨立的路徑，只是剛好放在同一個 `CsvUploadPanel.vue` 元件裡。

## 相關檔案

```
components/import/CsvUploadPanel.vue   # 呼叫端：定義範例內容、觸發下載
utils/downloadFile.ts                  # 共用工具：Blob + <a download> 下載機制
```

## 流程總覽

```
使用者點擊「下載範例檔」按鈕
    ↓
handleDownloadSample()  (CsvUploadPanel.vue)
    ↓
downloadTextFile(filename, content, mimeType)  (utils/downloadFile.ts)
    ↓
建立 Blob → URL.createObjectURL → 建立隱藏 <a download> → click() → 觸發瀏覽器下載
    ↓
移除暫時的 <a> 節點 → URL.revokeObjectURL() 釋放記憶體
```

## 各步驟細節

### 1. 呼叫端：`CsvUploadPanel.vue` 定義要下載的內容

範例 CSV 的內容是寫死在前端的字串常數，用陣列 + `join('\r\n')` 組成（`\r\n` 是 CSV 慣用的換行符號，確保 Excel 等試算表軟體開啟時不會有格式問題）：

```ts
const SAMPLE_CSV_CONTENT = [
  'expense_date,amount,category,invoice_number,note',
  '2026-07-01,150.00,餐飲,INV-0001,午餐',
  '2026-07-02,899.50,交通,,計程車資',
  '2026-07-03,45.00,餐飲,INV-0003,咖啡',
].join('\r\n')
```

按鈕點擊事件呼叫共用工具函式：

```ts
const handleDownloadSample = () => {
  downloadTextFile('expense_import_sample.csv', SAMPLE_CSV_CONTENT, 'text/csv;charset=utf-8;')
}
```

```vue
<Button icon="ios-download-outline" @click="handleDownloadSample">下載範例檔</Button>
```

傳入三個參數：
- `filename`：`'expense_import_sample.csv'`，下載後的檔案名稱。
- `content`：上面組好的 CSV 字串。
- `mimeType`：明確指定為 `'text/csv;charset=utf-8;'`（而非用 `downloadTextFile` 預設的 `text/plain`），讓瀏覽器 / OS 能正確辨識檔案類型。

### 2. 共用工具：`utils/downloadFile.ts` 的 `downloadTextFile`

```ts
const UTF8_BOM = '﻿'

export function downloadTextFile(filename: string, content: string, mimeType = 'text/plain;charset=utf-8;'): void {
  const blob = new Blob([UTF8_BOM + content], { type: mimeType })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  URL.revokeObjectURL(url)
}
```

逐行說明：

1. **`UTF8_BOM`**：在檔案內容前加上 UTF-8 BOM（Byte Order Mark，`﻿`）。這是因為 Excel（尤其是 Windows 版）開啟不含 BOM 的 UTF-8 CSV 時，容易誤判編碼導致中文字（例如「餐飲」「計程車資」）顯示成亂碼。加上 BOM 後 Excel 能正確辨識為 UTF-8。
2. **`new Blob([...], { type: mimeType })`**：把字串內容包裝成瀏覽器原生的 `Blob` 物件，並標記其 MIME type。
3. **`URL.createObjectURL(blob)`**：為這個 `Blob` 產生一個暫時的 `blob:` 協議 URL，可以像一般 URL 一樣被 `<a>`、`<img>` 等標籤引用，但只在當前分頁存活期間有效。
4. **建立隱藏的 `<a>` 標籤並設定 `download` 屬性**：
   - `link.href = url` 指向剛才建立的 Blob URL。
   - `link.download = filename` 是觸發「下載」而非「導覽」行為的關鍵屬性——瀏覽器看到 `download` 屬性會直接存檔，而不是嘗試在分頁中開啟內容。
   - 因為 `<a>` 元素沒有被實際渲染在畫面上（只是暫時插入 DOM 又立刻移除），使用者不會看到任何視覺變化，只會看到瀏覽器的下載提示 / 下載列。
5. **`link.click()`**：用程式觸發點擊，啟動下載，不需要使用者手動點這個隱藏的連結。
6. **`document.body.removeChild(link)`**：下載觸發後立刻把暫時建立的 `<a>` 節點從 DOM 移除，避免污染 DOM 樹。
7. **`URL.revokeObjectURL(url)`**：釋放瀏覽器為這個 Blob 保留的記憶體。若不呼叫，Blob URL 會一直佔用記憶體直到分頁關閉，是常見的記憶體洩漏來源；這裡在下載觸發後立即釋放是正確作法。

## 為什麼不呼叫後端 API？

範例檔內容是固定的靜態資料，屬於「前端展示範例格式」用途，不涉及使用者資料或資料庫查詢，因此不需要也不應該為此建立一支後端端點——直接在前端組字串下載即可，避免不必要的網路請求與後端負擔。若未來範例內容需要動態產生（例如依使用者當前有效類別產生範例），才需要評估改由後端 API 提供。

## 與其他下載情境的關係

`downloadTextFile` 是通用工具函式，目前僅被 `CsvUploadPanel.vue` 使用於下載範例 CSV。若之後有其他「純前端產生內容並下載」的需求（例如匯出目前頁面的表格資料為 CSV），可以直接複用這個函式；但若下載內容來自後端（例如伺服器產生的報表檔案），則應改用不同的下載方式（例如直接以 `<a>` 指向後端下載端點，或用 Axios 以 `responseType: 'blob'` 取得檔案後再走相同的 Blob → `<a download>` 流程），並在該情境下另外記錄說明。
