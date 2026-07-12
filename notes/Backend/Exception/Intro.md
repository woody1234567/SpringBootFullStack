# Exception 處理說明

## 架構總覽

```text
Repository (SimpleJdbcCall 呼叫 SQL Server Stored Procedure)
  ↓ result_code / result_message
Service (依 result_code 拋出對應的自訂 Exception)
  ↓
LoggingAspect (@Around 攔截 controller/service/repository 三層，統一記錄 ENTER/EXIT/EXCEPTION)
  ↓
GlobalExceptionHandler (@RestControllerAdvice 統一轉成標準 JSON 回應)
  ↓
前端收到 { success, message, data, errorCode, errors }
```

相關檔案：

- `backend/src/main/java/com/example/expensetracker/exception/BusinessException.java`
- `backend/src/main/java/com/example/expensetracker/exception/ValidationException.java`
- `backend/src/main/java/com/example/expensetracker/exception/UnauthorizedException.java`
- `backend/src/main/java/com/example/expensetracker/exception/ForbiddenException.java`
- `backend/src/main/java/com/example/expensetracker/exception/ResourceNotFoundException.java`
- `backend/src/main/java/com/example/expensetracker/exception/DuplicateResourceException.java`
- `backend/src/main/java/com/example/expensetracker/exception/DatabaseOperationException.java`
- `backend/src/main/java/com/example/expensetracker/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/example/expensetracker/aspect/LoggingAspect.java`
- `backend/src/main/java/com/example/expensetracker/dto/response/ApiResponse.java`
- `backend/src/main/java/com/example/expensetracker/service/ExpenseService.java`

## 1. 自訂例外類別 (`exception/`)

一組繼承 `RuntimeException` 的簡單類別，各自對應一種業務語意：

| 類別 | 對應 HTTP | errorCode |
|---|---|---|
| `ValidationException` | 400 | `VALIDATION_ERROR` |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` |
| `ForbiddenException` | 403 | `FORBIDDEN` |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` |
| `DuplicateResourceException` | 409 | `DUPLICATE` |
| `BusinessException` | 400 | `BUSINESS_ERROR` |
| `DatabaseOperationException` | 500 | `SYSTEM_ERROR`（隱藏細節） |

`DatabaseOperationException` 額外帶 `cause`，用來包裝底層 JDBC 例外，避免把 SQL Server 的細節洩漏出去。

## 2. Service 層：把 SQL Server 的 result_code 轉成例外

例如 `ExpenseService.applyMutationOutcome()`（`ExpenseService.java:78-89`）依照 SP 回傳的 `result_code` 做 switch，對應到 `.claude/CLAUDE.md` 第 7 節建議的語意（`SUCCESS` / `NOT_FOUND` / `DUPLICATE` / `VALIDATION_ERROR` ...），並拋出相應的自訂例外。查不到的物件則用 `Optional.orElseThrow(...)`（`ExpenseService.java:56-57`）。

這符合本專案規範：**Java 不重複 SQL Server 已做的業務驗證，只負責把資料庫結果翻譯成應用層例外**。

## 3. AOP 統一記錄 (`aspect/LoggingAspect.java`)

用 `@Around` 切入 controller、service、repository 三層所有 public 方法，取代逐一手寫的 `log.info/log.error`：

- 正常執行：記錄 `ENTER` / `EXIT`（含耗時、遮罩過的參數與結果）。
- 拋出例外時：
  - 若是**預期內**的業務例外（`EXPECTED_EXCEPTIONS` 集合：`ValidationException` / `ResourceNotFoundException` / `DuplicateResourceException` / `BadCredentialsException` / `UnauthorizedException` / `ForbiddenException` / `BusinessException`），用 `log.warn`，且**不印堆疊**（因為這是正常業務流程，不是系統錯誤）。
  - 其他未預期的例外（含 `DatabaseOperationException`、`IllegalStateException`、真正的系統錯誤）用 `log.error` 並印出完整堆疊。
- 不論哪種都會 `throw ex` 繼續往外拋，讓例外正常傳遞到 `GlobalExceptionHandler`。

三層各自記錄一次是刻意設計，所以同一個請求會看到 controller → service → repository 三組 ENTER/EXCEPTION，不是重複記錄的 bug。

## 4. `GlobalExceptionHandler`：統一轉換成 API 回應

用 `@RestControllerAdvice` + `@ExceptionHandler` 集中處理，每個自訂例外對應到固定 HTTP 狀態碼與 `ApiResponse`：

- Bean Validation 失敗（`MethodArgumentNotValidException`、`ConstraintViolationException`）→ 400 + 欄位錯誤清單。
- Spring Security 的 `BadCredentialsException` → 401，訊息刻意模糊為「Invalid email or password」，不透露帳號是否存在。
- 各自訂例外 → 對應狀態碼（見上表）。
- `MaxUploadSizeExceededException` → 400，統一回「Uploaded file is too large」。
- `DatabaseOperationException` → 500，`log.error` 記錄完整例外但**回給前端的訊息是固定的安全文字**，不洩漏 SQL 錯誤細節。
- 最後兜底 `Exception.class` → 500，同樣只回通用訊息，並記完整堆疊方便除錯。

回應格式統一用 `ApiResponse<T>`（`dto/response/ApiResponse.java`），符合 `.claude/CLAUDE.md` 第 15 節規定的成功/失敗格式（`success`, `message`, `data`, `errorCode`, `errors`）。

## 小結

這個設計完整落實了 `.claude/CLAUDE.md` 的規範：SQL Server 用 `result_code` 表達業務狀態 → Service 翻譯成 Java 例外（不重複驗證邏輯）→ AOP 統一記錄且區分「預期業務例外」與「系統例外」→ Controller 完全不碰例外處理，全部交給 `GlobalExceptionHandler` 統一轉成一致的 JSON 格式，且絕不洩漏資料庫內部細節給前端。
