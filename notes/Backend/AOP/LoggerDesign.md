# 後端 AOP —— 統一 Log 與敏感資料遮罩

相關檔案：

- `backend/src/main/java/com/example/expensetracker/aspect/LoggingAspect.java`
- `backend/src/main/java/com/example/expensetracker/aspect/LoggedOperation.java`
- `backend/src/main/java/com/example/expensetracker/util/SensitiveDataMasker.java`
- `backend/pom.xml`（`spring-boot-starter-aop`）

## 1. 為什麼要用 AOP

CLAUDE.md 第 13 節要求 Controller / Service / Repository 都要記錄方法進入、離開、耗時、例外，但如果每個方法都手動寫 `log.info`/`log.error`，會產生大量重複程式碼。這裡用 Spring AOP 把「進入 / 離開 / 例外」這個橫切關注點（cross-cutting concern）抽成一個切面，其餘三層完全不用自己寫 log 呼叫。

## 2. Pointcut —— 定義攔截範圍

`LoggingAspect.java:43-57`

```java
@Pointcut("execution(public * com.example.expensetracker.repository.impl..*.*(..))")
private void repositoryLayer() {}

@Pointcut("execution(public * com.example.expensetracker.service..*.*(..))")
private void serviceLayer() {}

@Pointcut("execution(public * com.example.expensetracker.controller..*.*(..))")
private void controllerLayer() {}

@Pointcut("repositoryLayer() || serviceLayer() || controllerLayer()")
private void loggedLayer() {}
```

`execution(public * package..*.*(..))` 表示「這個 package 底下（含子 package）任何類別的任何 public 方法，不管參數為何」都會被攔截。三個 pointcut 用 `||` 組合成 `loggedLayer()`，所以 `repository.impl`、`service`、`controller` 三個 package 底下的 public 方法全部會被套用同一支 advice。

## 3. `@Around` Advice —— 實際的 ENTER / EXIT / EXCEPTION 邏輯

`LoggingAspect.java:59-83`

```java
@Around("loggedLayer()")
public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String label = resolveLabel(signature);
    String args = SensitiveDataMasker.describeArgs(signature.getParameterNames(), joinPoint.getArgs());

    log.info("ENTER {} args=[{}]", label, args);
    long startNanos = System.nanoTime();
    try {
        Object result = joinPoint.proceed();
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("EXIT {} durationMs={} result={}", label, durationMs, SensitiveDataMasker.describeResult(result));
        return result;
    } catch (Throwable ex) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        if (EXPECTED_EXCEPTIONS.contains(ex.getClass())) {
            log.warn("EXCEPTION {} durationMs={} args=[{}] exceptionType={} message={}",
                    label, durationMs, args, ex.getClass().getSimpleName(), ex.getMessage());
        } else {
            log.error("EXCEPTION {} durationMs={} args=[{}] exceptionType={} message={}",
                    label, durationMs, args, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
        throw ex;
    }
}
```

Spring 啟動時會替 `repository.impl`、`service`、`controller` 底下符合 pointcut 的 bean 建立動態代理。當外部呼叫某個方法時，實際上會先進到這個 `@Around` advice：

1. 記錄 `ENTER`（含方法名稱與參數，參數已先經過遮罩）
2. 呼叫 `joinPoint.proceed()`，也就是真正執行原本的方法邏輯
3. 正常回傳就記 `EXIT`（含耗時 `durationMs`、回傳值描述）
4. 拋出例外就記 `EXCEPTION`：
   - 屬於 `EXPECTED_EXCEPTIONS`（`ValidationException`、`ResourceNotFoundException`、`DuplicateResourceException`、`BadCredentialsException`、`UnauthorizedException`、`ForbiddenException`、`BusinessException`）的預期業務例外，用 `log.warn`，不印完整 stacktrace
   - 其他非預期例外用 `log.error`，印完整 stacktrace 方便追查

因為 controller → service → repository 三層都各自符合 pointcut，若 controller 方法內部呼叫了 service、service 又呼叫了 repository，這三層各自的 ENTER/EXIT 都會各記一次。一次請求的 log 因此會呈現「巢狀」的三層記錄，這是刻意設計、不是重複記錄。

## 4. `@LoggedOperation` —— 讓 Repository 的 log 對應到真正的 SQL Server 物件

`LoggedOperation.java`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LoggedOperation {
    String value();
}
```

Repository 方法預設會顯示 `類別名.方法名`（例如 `CategoryRepositoryImpl.getActiveCategories`），但這對應不到背後呼叫的是哪個 SQL Server 物件。所以在 Repository 方法上標註這個註解，直接寫上 schema-qualified 的物件名稱：

```java
@Override
@LoggedOperation("app_expense.v_active_categories")
public List<CategoryRow> getActiveCategories() {
    return jdbcTemplate.query(SELECT_ACTIVE_CATEGORIES, new CategoryRowMapper());
}
```

`resolveLabel()`（`LoggingAspect.java:85-91`）會用反射檢查方法上有沒有這個註解：

```java
private String resolveLabel(MethodSignature signature) {
    LoggedOperation annotation = signature.getMethod().getAnnotation(LoggedOperation.class);
    if (annotation != null) {
        return annotation.value();
    }
    return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
}
```

有標註就直接用 `annotation.value()` 取代 Java 方法名作為 log 的 label；沒標註（例如 Service、Controller 方法）則 fallback 回 `類別名.方法名`。這讓 log 能直接對應到 CLAUDE.md 第 6 節提到的 stored procedure / view / function 名稱，符合第 13 節「Log SQL Server Stored Procedure or Function name」的要求。目前已在 `CategoryRepositoryImpl`、`ExpenseRepositoryImpl`、`UserRepositoryImpl`、`ExpenseImportRepositoryImpl` 使用。

## 5. `SensitiveDataMasker` —— 避免敏感資料寫進 log

`@Around` advice 組裝 `args=[...]` 與 `result=...` 字串時，不是直接呼叫物件的 `toString()`（避免未來新增欄位不小心洩漏敏感值），而是透過這支工具類別：

```java
log.info("ENTER {} args=[{}]", label, SensitiveDataMasker.describeArgs(signature.getParameterNames(), joinPoint.getArgs()));
...
log.info("EXIT {} ... result={}", label, ..., SensitiveDataMasker.describeResult(result));
```

`SensitiveDataMasker.java` 的遮罩規則：

- 參數 / 欄位名稱（不分大小寫）含 `password`、`token`、`jwt`、`secret`、`hash`、`sessionid`、`credential`、`authorization` → 整個值變成 `***`
- 名稱含 `email` → 只保留首字元與網域，例如 `j***@gmail.com`
- 基本型別（`CharSequence`、`Number`、`Boolean`、`Enum`、`LocalDate`、`LocalDateTime`、`BigDecimal`）直接印值
- `Optional`：遞迴描述內部值（空的印 `empty`）
- `Map`：只印 `Map[size=n]`，不印內容
- `Collection`：只印 `類別名[size=n]`
- `MultipartFile`：只印檔名與檔案大小，不印內容
- `ResponseEntity`：印 HTTP status，並遞迴描述 body
- Java `record`：用反射逐一取出各 component 遞迴描述（同樣會套用上述遮罩規則），避免呼叫 record 自己的 `toString()`
- 其餘物件：只印類別名稱，不深入內容
- 有 `MAX_DEPTH = 4` 的遞迴深度上限，避免物件圖過深或循環參照造成問題

這符合 CLAUDE.md 第 13 節「Do not log plaintext passwords / password hashes / JWT tokens / session IDs / confidential PII」的規範。

## 6. 生效前提

`pom.xml` 已引入 `spring-boot-starter-aop`（同時帶入 AspectJ runtime 與 Spring AOP 自動代理支援）。`LoggingAspect` 標了 `@Component` + `@Aspect`，Spring Boot 的 auto-configuration 會自動偵測並啟用動態代理，不需要額外手動加 `@EnableAspectJAutoProxy`。

## 完整請求流程

```
Controller 方法被呼叫
    ↓ 被 LoggingAspect 攔截 (pointcut: controllerLayer)
    ↓ log ENTER Controller.method args=[已遮罩]
    ↓ joinPoint.proceed() 執行原本邏輯
    ↓     內部呼叫 Service 方法
    ↓         ↓ 被 LoggingAspect 攔截 (pointcut: serviceLayer)
    ↓         ↓ log ENTER Service.method
    ↓         ↓ joinPoint.proceed()
    ↓         ↓     內部呼叫 Repository 方法
    ↓         ↓         ↓ 被 LoggingAspect 攔截 (pointcut: repositoryLayer)
    ↓         ↓         ↓ log ENTER <@LoggedOperation 標註的 SQL 物件名稱 或 類別.方法>
    ↓         ↓         ↓ joinPoint.proceed() 執行 SQL Server 呼叫
    ↓         ↓         ↓ log EXIT / EXCEPTION
    ↓         ↓ log EXIT / EXCEPTION
    ↓ log EXIT / EXCEPTION
回傳給呼叫端 / 拋出例外往上層傳遞
```

## 注意事項

- 三層各自獨立記錄 ENTER/EXIT 是刻意設計，方便從 log 還原一次請求在各層停留的時間與參數，不需要因為「看起來重複」而移除。
- 只有 Repository 層需要標 `@LoggedOperation`；Service、Controller 沒標註時會 fallback 用 `類別名.方法名`，不用特地補標。
- 新增例外類別時，若屬於「預期的業務例外」（回應給前端合理的 4xx），記得加進 `LoggingAspect.EXPECTED_EXCEPTIONS`，否則會被當成非預期例外印出完整 stacktrace，log 會比較吵。
- 新增 DTO 或參數時若欄位名稱含敏感關鍵字（`password`、`token`、`email` 等），`SensitiveDataMasker` 會自動遮罩，不需要在 Service/Controller 自行處理遮罩邏輯。
