# SensitiveDataMasker 實作邏輯

檔案位置：`backend/src/main/java/com/example/expensetracker/util/SensitiveDataMasker.java`

## 用途

供 `com.example.expensetracker.aspect.LoggingAspect`（AOP 日誌切面）使用，在把方法參數 / 回傳值寫進 log 之前先做遮蔽，避免明文密碼、密碼雜湊、JWT token、session ID 等敏感資訊外洩到日誌中（對應 `.claude/CLAUDE.md` 第 13 節的規範）。

純靜態工具類：`final class` + 私有建構子，不可被實例化。

## 入口方法

- `describeArgs(String[] paramNames, Object[] args)`：把整組方法參數組成 `name=value, name=value...` 字串，供 log 使用。
- `describeResult(Object value)`：描述單一回傳值。
- 兩者都委派給私有遞迴方法 `describe(String name, Object value, int depth)`。

## 核心遞迴 `describe`

依序判斷（第 61-96 行）：

1. `null` → 回傳 `"null"`。
2. 深度超過 `MAX_DEPTH = 4` → 回傳 `ClassName[...]`，避免物件圖太深或循環參照造成遞迴爆炸。
3. **參數/欄位名稱本身就是敏感欄位**（見下方 `isSensitiveName`）→ 呼叫 `maskValue` 遮蔽，不論值的型別為何。
4. 常見「安全」基本型別（`CharSequence`、`Number`、`Boolean`、`Enum`、`LocalDate`、`LocalDateTime`、`BigDecimal`）→ 直接 `String.valueOf(value)` 印出。
5. `Optional` → 遞迴描述內部值，或印 `"empty"`。
6. `Map` / `Collection` → 只印大小（例如 `Map[size=3]`），不印內容，避免洩漏集合內的敏感資料。
7. `MultipartFile` → 只印檔名與大小，不印內容。
8. `ResponseEntity` → 印出 HTTP 狀態碼，並遞迴描述 body。
9. **Java Record** → 用反射列出每個 record component 並遞迴描述（見 `describeRecord`）。
10. 其他任意物件（例如一般 DTO class）→ 只印 `ClassName`，**刻意不呼叫該物件自己的 `toString()`**，因為無法保證該 class 內部沒有藏敏感欄位（例如日後新增的 record component）。

## 敏感欄位判斷 `isSensitiveName`

第 116-130 行：

- 名稱轉小寫後，只要包含 `"email"`，或包含 `FULL_MASK_KEYWORDS` 集合中的關鍵字：
  `password`、`token`、`jwt`、`secret`、`hash`、`sessionid`、`credential`、`authorization`
  即視為敏感欄位。
- 這是「依欄位名稱」的黑名單式判斷，不是依型別判斷 —— 只要參數或 record component 名稱含這些關鍵字（例如 `resetToken`、`passwordHash`），即使值是普通字串也會被擋下遮蔽。

## 遮蔽策略 `maskValue`

第 132-138 行：

- 欄位名稱含 `"email"` 且值是 `String` → 呼叫 `maskEmail` 做部分遮蔽（保留第一個字元 + `@` 之後的網域，例如 `j***@gmail.com`）。
- 其餘一律回傳固定字串 `"***"`（完全遮蔽，不留任何原始資訊，例如 token、密碼雜湊）。

## Record 遞迴 `describeRecord`

第 98-114 行：

- 用 `record.getClass().getRecordComponents()` 反射列出每個 component。
- 對每個 component 呼叫 `component.getAccessor().invoke(record)` 取值，再遞迴丟進 `describe`（帶入 component 名稱，讓敏感欄位偵測生效），並將 `depth + 1` 傳下去控制遞迴深度。
- 若反射失敗（`ReflectiveOperationException`）→ 該欄位印 `<unreadable>`，不拋出例外中斷整個 log 流程。

## 設計重點

- 策略：「白名單型別直接印、黑名單欄位名一律遮蔽、其餘未知型別只印 class name」。
- 用欄位名稱關鍵字比對而非硬編碼特定 DTO 欄位，讓新增欄位（如未來新的 record component）也能自動被涵蓋 —— 屬於防禦性設計，寧可過度遮蔽也不要漏掉敏感資訊。
- 不直接呼叫物件的 `toString()`，避免因為呼叫端不知情的自訂 `toString()` 實作而洩漏敏感欄位。
