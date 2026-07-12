# ExpenseListView.vue 元件溝通說明

`ExpenseListView.vue` 是消費紀錄頁面的「容器/協調者」，本身幾乎不含 UI 細節，而是透過 **props（父→子傳資料）** 和 **emit 事件（子→父傳動作）** 跟四個子元件溝通。整體是標準的 Vue 3 單向資料流。

## 1. ExpenseFilterBar（篩選列）

```html
<ExpenseFilterBar
  v-model:date-from="dateFrom"
  v-model:date-to="dateTo"
  v-model:category-id="categoryId"
  :categories="categories"
  @search="handleSearch"
  @reset="handleResetFilters"
/>
```

- **雙向綁定（`v-model:xxx`）**：`dateFrom`、`dateTo`、`categoryId` 來自 `useExpenseFilters()` composable。`ExpenseFilterBar.vue` 內用 `defineModel()` 宣告對應的 model，使用者在下拉選單/日期選擇器操作時，值會直接同步回父層的 ref，不需要額外 emit。
- **單向 prop**：`categories` 從父層的 `categories` ref 傳入，供下拉選單顯示分類選項。
- **事件**：子元件只在按下「查詢」「重設」按鈕時 emit `search` / `reset`，父層才真正呼叫 `handleSearch` / `handleResetFilters` 去打 API。也就是說「輸入條件」和「觸發查詢」是分開的兩件事。

## 2. ExpenseTable（列表）

```html
<ExpenseTable :expenses="expenses" :loading="loading" @edit="openEditModal" @delete="handleDelete" />
```

- **純 props 傳入**：`expenses`（資料陣列）、`loading`（是否顯示 `Spin`）。`ExpenseTable` 完全是展示型元件，沒有自己的 API 呼叫或狀態。
- **事件回傳整筆物件**：點「編輯」emit `edit(expense)`，點「刪除」emit `delete(expense)`，父層拿到完整的 `Expense` 物件後分別呼叫 `openEditModal` 開 modal，或 `handleDelete` 跳出確認框並打刪除 API。

## 3. ExpenseFormModal（新增/編輯彈窗）

```html
<ExpenseFormModal
  v-model="modalVisible"
  :categories="categories"
  :editing="editingExpense"
  @submit="handleSubmit"
/>
```

- **`v-model`（等於 `modelValue` + `update:modelValue`）**：控制 modal 開關。父層設定 `modalVisible.value = true` 打開；modal 內按取消/關閉時 emit `update:modelValue(false)` 讓父層同步關閉。
- **`editing` prop**：傳入 `editingExpense`（null 代表新增模式，有值代表編輯模式）。子元件內部用 `watch(() => props.modelValue, ...)` 在打開時依此初始化表單內容。
- **`submit` 事件**：子元件做完前端驗證（View UI Plus 的 `Form.validate`）後，emit `submit(form)` 把整理好的表單資料丟給父層。父層的 `handleSubmit` 才決定要呼叫 `createExpense` 還是 `updateExpense`（依 `editingExpense` 是否存在判斷），成功後關閉 modal 並重新 `loadExpenses()`。

## 4. Page（View UI Plus 分頁元件，非自製元件）

```html
<Page :total="totalCount" :current="page" :page-size="pageSize" show-total @on-change="handlePageChange" />
```

- 純展示分頁狀態，`@on-change` 觸發 `handlePageChange`，更新 `page.value` 後重新 `loadExpenses()`。

## 整體資料流總結

- **狀態集中在父層**：`expenses`、`categories`、`loading`、`modalVisible`、`editingExpense`，以及來自 `usePagination` / `useExpenseFilters` composable 的分頁與篩選狀態，全部由 `ExpenseListView` 持有。
- **子元件不打 API**：所有 `expenseApi` / `categoryApi` 呼叫都集中在父層，子元件（`ExpenseFilterBar`、`ExpenseTable`、`ExpenseFormModal`）只負責顯示 props 與 emit 使用者操作，符合專案慣例中「API 呼叫集中在 API 層，UI 元件不直接處理業務邏輯」的原則。
- **典型循環**：使用者操作子元件 → emit 事件 → 父層 handler 呼叫 API → 更新父層 ref（`expenses`/`totalCount`/`modalVisible` 等）→ 子元件因 props 改變自動重新渲染。
