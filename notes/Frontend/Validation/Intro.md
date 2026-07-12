# 前端資料驗證現況

記錄目前前端對「使用者輸入」實際做了哪些驗證，分成兩大類：

1. **單筆表單驗證**：登入、註冊、新增/編輯消費紀錄 —— 使用 View UI Plus 的 `Form` + `rules`，做欄位層級（必填、型別、長度）的早期防護。
2. **CSV 批次匯入（表格資料）**：只做「副檔名」檢查，**不解析、不驗證 CSV 內容**；逐列的格式與業務規則驗證完全交給後端 / SQL Server，前端只負責呈現後端回傳的逐列錯誤。

這個分工符合 `.claude/CLAUDE.md` 的架構原則：業務驗證規則主要放在 SQL Server / 後端，前端驗證僅用於提升使用者體驗、提早攔截明顯錯誤，不是安全或正確性的最終邊界。

## 相關檔案

```
types/form/viewUiForm.ts               # 共用型別：ViewUiFormInstance / ViewUiFormRules
views/auth/LoginView.vue               # 登入表單驗證
views/auth/RegisterView.vue            # 註冊表單驗證（含自訂 validator）
components/expense/ExpenseFormModal.vue # 新增/編輯消費紀錄表單驗證
components/expense/ExpenseTable.vue    # 唯讀顯示表格，無輸入、無驗證
components/import/CsvUploadPanel.vue   # CSV 匯入：僅檔案副檔名檢查
components/import/ImportResultTable.vue # 呈現後端回傳的逐列錯誤
```

## 1. 共用型別：`types/form/viewUiForm.ts`

因為 `view-ui-plus` 沒有匯出 `Form` 元件實例與驗證規則的 TypeScript 型別，專案自行宣告了一個「只涵蓋目前實際用到的欄位」的最小型別：

```ts
export interface ViewUiFormInstance {
  validate: (callback: (valid: boolean) => void) => void
}

export interface ViewUiFormRuleItem {
  required?: boolean
  type?: string
  min?: number
  max?: number
  message?: string
  trigger?: string
  validator?: (rule: unknown, value: string, callback: (error?: Error) => void) => void
}

export type ViewUiFormRules = Record<string, ViewUiFormRuleItem[]>
```

所有表單（Login / Register / ExpenseFormModal）都共用這組型別定義 `rules`，並透過 `formRef.value?.validate(callback)` 觸發驗證。

## 2. 各表單目前的驗證規則

### `LoginView.vue`

| 欄位 | 規則 |
|---|---|
| `email` | 必填、`type: 'email'`（格式檢查） |
| `password` | 必填 |

### `RegisterView.vue`

| 欄位 | 規則 |
|---|---|
| `email` | 必填、`type: 'email'` |
| `password` | 必填、`min: 8`（至少 8 字元） |
| `confirmPassword` | 必填、自訂 `validator` 檢查是否與 `password` 相同 |
| `displayName` | 選填，**無任何規則** |

自訂 validator 範例（兩次密碼一致性檢查，非 View UI Plus 內建規則類型可涵蓋）：

```ts
const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('兩次輸入的密碼不一致'))
    return
  }
  callback()
}
```

### `ExpenseFormModal.vue`（新增/編輯消費紀錄）

| 欄位 | 規則 | 備註 |
|---|---|---|
| `expenseDate` | 必填 | `Input` 純文字輸入，**未做日期格式（yyyy-MM-dd）檢查**，僅靠 placeholder 提示格式 |
| `amount` | 必填、`type: 'number'` | 另外 `InputNumber` 元件加了 `:min="0.01"`，屬於元件層級限制而非 `rules` |
| `categoryId` | 必填、`type: 'number'` | |
| `invoiceNumber` | **無規則** | 後端規格為「最長 20 字元、不可重複」，前端未做長度或重複檢查 |
| `note` | **無規則** | 後端規格為「最長 500 字元」，前端未做長度檢查 |

## 3. CSV 批次匯入（表格資料）：只驗證副檔名

`CsvUploadPanel.vue` 的 `handleBeforeUpload` 是前端對 CSV 匯入唯一的驗證：

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

- 只檢查**檔名副檔名**是否為 `.csv`，不檢查 MIME type，也不讀取檔案內容。
- 面板上的 `<Alert>` 區塊雖然以表格列出各欄位規格（`expense_date` 日期格式、`amount` 數字格式、`category` 須存在、`invoice_number` 長度與重複性、`note` 長度上限），但這些**只是給使用者參考的說明文字**，前端完全沒有依此規格解析並預先驗證 CSV 內容。
- 實際的逐列驗證（日期格式、金額格式、類別是否存在啟用中、發票號是否重複、欄位長度等）由後端 / SQL Server 執行，且整批匯入是單一交易：任一列失敗，整批都不會寫入。
- 前端 `ImportResultTable.vue` 只負責把後端回傳的 `ImportResultData`（含 `failedRows: { rowNumber, message }[]`）原樣呈現，不做二次判斷或計算。

詳細的上傳/下載流程分層說明見 [Upload.md](../Files/Upload.md) 與 [Download.md](../Files/Download.md)。

## 4. `ExpenseTable.vue`：唯讀顯示，無驗證

`ExpenseTable.vue` 只負責渲染 `expenses` 陣列（日期、分類、金額、發票號、備註 + 編輯/刪除按鈕），沒有任何輸入欄位，因此不涉及驗證問題。編輯操作是點擊「編輯」後開啟 `ExpenseFormModal.vue`（見上方第 2 節），驗證邏輯都在該表單元件裡，而不是在表格本身。

## 5. 現況總結

| 輸入來源 | 前端驗證程度 | 實際驗證權責 |
|---|---|---|
| Login / Register 表單 | 必填、型別（email）、長度（密碼 min 8）、自訂一致性檢查 | 前端把關基本格式，後端仍需重新驗證帳密 |
| 消費紀錄新增/編輯表單 | 必填、型別（number），**無長度/格式規則** | 後端 / SQL Server 負責業務規則（如發票號重複） |
| CSV 批次匯入（表格資料） | **僅副檔名檢查**，不解析內容 | 後端 / SQL Server 逐列驗證，整批交易 |

**目前最大的落差**：CSV 匯入沒有任何前端逐列預檢，使用者要等整批送出、後端處理完才知道哪幾列有誤，對筆數多或格式錯誤多的檔案而言，來回修正的體驗較差。若之後要補強，建議方向：

- 前端可在上傳前用 `FileReader` / CSV parser 解析內容，做「必填欄位是否存在、日期格式是否為 yyyy-MM-dd、金額是否為正數且最多 2 位小數」等**格式層級**的預檢，並在送出前提示使用者。
- 不應在前端重做「類別是否存在啟用中」「發票號是否與歷史紀錄重複」這類**業務規則**檢查（這類規則依賴資料庫狀態，屬於後端/資料庫層職責，前端做了也只是重複且容易與後端不一致）。
- `ExpenseFormModal.vue` 的 `invoiceNumber` / `note` 可以補上 `max` 長度規則（20 / 500），與後端規格對齊，屬於低成本、高價值的補強。
