# repository/model 的作用

`repository/model/` 存放 **Repository 層專用的資料承載物件（record）**，作用是把 SQL Server 呼叫（Stored Procedure / Function）的原始結果，轉換成型別安全的 Java 物件，再往上傳給 Service 層。

## 兩種典型用途

1. **對應 Stored Procedure 的 OUTPUT 參數 / 執行結果**（依照 `CLAUDE.md` 第 7 節的慣例：`result_code` / `result_message` / `created_id`）
   - `CreateExpenseResult(expenseId, resultCode, resultMessage)`
   - `CreateUserResult`、`MutationResult` 同樣模式
   - `ImportBatchResult(batchId, successCount, resultCode, resultMessage, failedRows)` 是批次匯入的彙總結果，內含 `List<ImportRowFailure>`（`ImportRowFailure(rowNumber, errorMessage)`）代表每一列失敗的明細

2. **對應資料表 / View / TVF 查詢出來的一列資料（Row Mapper 的目標型別）**
   - `UserRow(userId, email, passwordHash, displayName, active)`
   - `CategoryRow`、`ExpenseRow`、`FindUserResult`、`GetExpenseResult`、`SearchExpensesResult` 也是同一類

以及 `ParsedExpenseImportRow`，是匯入 CSV 時、經過解析後、準備傳給 Stored Procedure 的中介物件。

## 在分層架構中的定位

對應 `CLAUDE.md` 第 5.3 節 Repository 層職責：

```text
SQL Server SP / Function / View
        ↓ (RowMapper / SimpleJdbcCall 輸出)
repository/model/*.java   ← 這裡：原始資料庫結果的 Java 表示
        ↓ (Service 層轉換)
dto/response/*.java       ← 對外的 API 回應 DTO
```

這些 record **不應該直接回傳給 Controller / 前端**——依照專案規範，Service 層要把它們轉換成 `dto/response` 底下的 API 回應 DTO，避免把資料庫層的內部結構（如 `resultCode`、`passwordHash`）洩漏到 API 合約中。

專案中 `AuthService.java` 的做法是正確示範：從 `FindUserResult` 取出 `UserRow` 後只在 Service 內部比對密碼（`AuthService.java:49-51`），不會讓它流出這一層。

---

# 洩漏資料庫層內部結構到 API 會怎麼樣？

直接把 `repository/model` 的物件回傳給 Controller/前端，會造成幾類實際問題：

## 1. 敏感資料外洩（安全性）

`UserRow` 裡有 `passwordHash` 欄位。如果 Controller 直接回傳 `UserRow`（例如序列化成 JSON），前端 response 就會包含密碼雜湊值——即使前端不會顯示它，它也會出現在 network tab、log、瀏覽器快取裡，等於把攻擊面暴露出去。

## 2. 內部實作細節外洩

`resultCode`（如 `DUPLICATE`、`SYSTEM_ERROR`）、`batchId`、SP 的參數命名等都是資料庫層的內部語言。一旦這些欄位變成 API 合約的一部分：

- 前端會開始依賴這些字串做邏輯判斷（例如 `if (data.resultCode === 'DUPLICATE')`），但這些值其實是 SQL Server SP 的輸出慣例，不是設計給外部消費的穩定介面。
- 攻擊者能藉由錯誤訊息推測資料庫結構（schema 名稱、SP 名稱），這正是 `CLAUDE.md` 第 8 節明文禁止的「Do not expose sensitive database details, schema names, SQL statements... through API responses」。

## 3. API 合約與資料庫結構被綁死（可維護性）

如果 Controller 回傳的就是 `repository/model`，那麼哪天要幫某個 Stored Procedure 加一個 OUTPUT 參數、改欄位名稱，或把某個查詢從 SP 換成 View，前端 TypeScript 型別就會跟著炸掉——即使那個改動跟業務語意完全無關。DTO 這一層存在的目的就是隔離「資料庫怎麼存」跟「API 長什麼樣」，讓兩邊可以獨立演進。

## 4. 回應格式不一致

專案規定的統一回應格式（`CLAUDE.md` 第 15 節）是 `{ success, message, data }`。`repository/model` 的 record 是給 RowMapper/SimpleJdbcCall 用的原始結構，並不是為了符合這個 API 包裝格式設計的，直接回傳等於繞過了這層規範，前端每個 API 都要各自猜格式。

## 正確做法

Service 層把 `UserRow`/`CreateUserResult` 轉換成 `dto/response/UserResponse` 之類的物件，只挑出前端該知道的欄位（如 `userId`、`email`、`displayName`），過濾掉 `passwordHash`、`resultCode` 這類內部資訊。
