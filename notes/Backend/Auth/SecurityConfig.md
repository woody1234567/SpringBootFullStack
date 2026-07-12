# SecurityConfig 逐項解析

檔案：`backend/src/main/java/com/example/expensetracker/config/SecurityConfig.java`

整體採用 **JWT + Stateless（無狀態）** 驗證模式，符合純 REST API 後端的常見架構。與 [Intro.md](./Intro.md) 的整體 JWT 架構總覽相對應，本篇專注在 `SecurityConfig` 這個類別本身的每一段設定。

## 1. 建構子與依賴注入（第 33–45 行）

```java
private final JwtTokenProvider jwtTokenProvider;
private final ObjectMapper objectMapper;
private final String allowedOrigins;
```

- 使用**建構子注入**（優於欄位注入 `@Autowired`）
- `jwtTokenProvider`：用來產生/解析 JWT token
- `objectMapper`：Jackson，用來把錯誤回應序列化成 JSON
- `allowedOrigins`：從 `application.yml` 的 `app.cors.allowed-origins` 注入，允許外部化設定 CORS 白名單（逗號分隔多個網域）

## 2. 密碼加密器（第 47–50 行）

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

使用 BCrypt 對密碼做雜湊，密碼永不明文儲存。

## 3. 核心過濾鏈設定（第 52–76 行）

```java
boolean isDevProfile = environment.acceptsProfiles(Profiles.of("dev", "local"));
```

先判斷目前是否為 `dev` 或 `local` profile，用來決定是否開放 Swagger 文件端點。

| 設定 | 說明 |
|---|---|
| `.csrf(csrf -> csrf.disable())` | 停用 CSRF 防護。因為是 stateless JWT API（不依賴 Cookie/Session），CSRF 攻擊風險不適用，是常見且合理的做法 |
| `.cors(...)` | 啟用 CORS，套用下方 `corsConfigurationSource()` 定義的規則 |
| `.sessionManagement(... STATELESS)` | 不建立 HttpSession，每次請求都靠 JWT 驗證身份 |
| `.authorizeHttpRequests(...)` | 定義路徑層級的授權規則（見下） |
| `.exceptionHandling(...)` | 未驗證時的自訂回應處理 |
| `.addFilterBefore(...)` | 插入自訂的 `JwtAuthenticationFilter` |

### 授權規則細節（第 60–71 行）

```java
auth.requestMatchers("/api/auth/**").permitAll();
if (isDevProfile) {
    auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll();
}
auth.anyRequest().authenticated();
```

- `/api/auth/**`（登入、註冊等）永遠開放，不需要先驗證 — 合理，因為使用者要先登入才能拿到 token
- Swagger/OpenAPI 文件**只有在 dev/local 環境**才開放，正式環境（prod）會要求驗證，避免正式環境洩漏 API 文件
- 其餘所有請求都必須通過身份驗證

## 4. 未驗證時的錯誤處理（第 78–87 行）

```java
private void handleUnauthenticated(...) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    ApiResponse<Void> body = ApiResponse.error("Authentication required", "UNAUTHORIZED", null);
    response.getWriter().write(objectMapper.writeValueAsString(body));
}
```

當未帶 token 或 token 無效時觸發，回傳 401 並輸出符合專案統一格式的 `ApiResponse` JSON 錯誤回應，而不是 Spring Security 預設的 HTML 錯誤頁。

## 5. CORS 設定（第 89–98 行）

```java
configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

- 允許的來源網域從設定檔讀取
- 允許的 HTTP 方法：GET/POST/PUT/DELETE/OPTIONS（**沒有 PATCH**，之後若新增 PATCH 端點要記得補上）
- 只開放 `Authorization`（帶 JWT）和 `Content-Type` 這兩個 header，屬於較嚴謹的最小權限設定

## 小結：請求流程

```text
請求 → CORS 檢查 → JwtAuthenticationFilter（解析/驗證 token）→ 授權規則檢查 → Controller
```

沒有 `UsernamePasswordAuthenticationFilter` 的表單登入邏輯，登入本身走 `/api/auth/**` 底下自訂的 Controller（呼叫 Service 驗證帳密、產生 JWT），對應 [Intro.md](./Intro.md) 中的 JWT 簽發流程。專案明確是走 JWT 而非 Session 驗證。
