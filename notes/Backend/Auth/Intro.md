# Security / JWT 實作說明

## 架構總覽

```text
Client
  ↓ POST /api/auth/login or /register
AuthController → AuthService → UserRepository (SP: app_user.create_user / findByEmail)
  ↓ 產生 JWT
JwtTokenProvider.generateToken()
  ↓ 之後每個請求
JwtAuthenticationFilter (OncePerRequestFilter) → 驗證 JWT → 寫入 SecurityContext
```

相關檔案：

- `backend/src/main/java/com/example/expensetracker/security/JwtTokenProvider.java`
- `backend/src/main/java/com/example/expensetracker/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/example/expensetracker/security/AuthenticatedUser.java`
- `backend/src/main/java/com/example/expensetracker/config/SecurityConfig.java`
- `backend/src/main/java/com/example/expensetracker/service/AuthService.java`
- `backend/src/main/java/com/example/expensetracker/controller/AuthController.java`
- `backend/src/main/resources/application.yml` / `application-example.yml`

## 1. JWT 簽發 (`JwtTokenProvider`)

- 使用 **jjwt** 函式庫 (`io.jsonwebtoken`)
- 建構子從 `application.yml` 注入 `app.jwt.secret`、`app.jwt.expiration-ms`，用 `Keys.hmacShaKeyFor(...)` 把字串密鑰轉成 `SecretKey`，代表演算法是 **HMAC-SHA (HS256/384/512，依 key 長度自動決定)**
- `generateToken(userId, email)`：
  - `subject` = userId
  - 自訂 claim `email`
  - `issuedAt` / `expiration`（目前設定 `86400000` ms = 24 小時，可用環境變數 `JWT_SECRET` / `JWT_EXPIRATION_MS` 覆蓋）
  - `signWith(signingKey)` 簽章後 `.compact()` 輸出字串
- `parseToken(token)`：用同一把 key 驗證簽章並解析 claims，失敗（過期、簽章不符等）回傳 `Optional.empty()` 而不是拋例外往外洩漏細節

**簽發時機**：`AuthService.register()` 和 `AuthService.login()` 在密碼用 `BCryptPasswordEncoder` 驗證/雜湊通過、且透過 SQL Server 預存程序 (`app_user.create_user`) 或查詢 (`findByEmail`) 拿到使用者資料後，呼叫 `jwtTokenProvider.generateToken(userId, email)`，回傳 `AuthResponse(token, user)` 給前端。這符合本專案「業務邏輯留在 SQL Server，Service 層只做應用編排」的原則。

## 2. 每次請求的驗證 (`JwtAuthenticationFilter`)

- 繼承 `OncePerRequestFilter`，從 `Authorization: Bearer <token>` header 取出 token
- 呼叫 `jwtTokenProvider.parseToken(token)`，成功則包成 `AuthenticatedUser(userId, email)` record 當作 principal，塞進 `UsernamePasswordAuthenticationToken`，寫入 `SecurityContextHolder`
- 目前寫死授權為 `ROLE_USER`（單一角色，沒有從 DB 查角色）
- 沒有 token 或驗證失敗就直接放行到下一個 filter，交給後面的 `authorizeHttpRequests` 規則擋下（未認證會走到 `authenticationEntryPoint`）

## 3. 安全設定 (`SecurityConfig`)

- **Stateless**：`SessionCreationPolicy.STATELESS`，因此是純 JWT 而非 Session 驗證
- **CSRF disabled**（stateless API 不需要）
- **CORS**：從 `app.cors.allowed-origins` 讀取允許來源
- `/api/auth/**` 全開放（註冊/登入不需先登入），dev/local profile 額外開放 Swagger
- 其餘路徑一律 `authenticated()`
- `JwtAuthenticationFilter` 用 `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` 插入 filter chain，取代 Spring Security 預設的表單登入流程
- 未認證時自訂 `authenticationEntryPoint`，回傳專案統一的 `ApiResponse` 錯誤格式（401 + `UNAUTHORIZED`），而不是 Spring 預設的 HTML 錯誤頁

## 密碼與密鑰

- `PasswordEncoder` bean 用 `BCryptPasswordEncoder`
- JWT 簽章密鑰目前在 `application.yml` 有寫死的預設值（`${JWT_SECRET:a3ba1a0f...}`），僅在沒設定環境變數 `JWT_SECRET` 時當作 fallback；`application-example.yml` 沒有 fallback，強制要求設定環境變數 —— 生產環境務必用環境變數覆蓋而不要依賴 yml 裡的預設值。

## 未來可擴充方向

- 角色/權限目前寫死 `ROLE_USER`，若要支援多角色需從 DB 查詢並放入 JWT claim 或每次查詢
- 目前沒有 refresh token 機制，token 過期後需重新登入
