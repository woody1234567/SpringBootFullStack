# JwtTokenProvider 逐項解析

檔案：`backend/src/main/java/com/example/expensetracker/security/JwtTokenProvider.java`

這是專案中唯一負責 **簽發（generate）** 與 **驗證/解析（parse）** JWT 的元件，被 `AuthService`（登入/註冊成功後產生 token）與 [JwtAuthenticationFilter.md](./JwtAuthenticationFilter.md)（每次請求解析 token）共用。整體流程對應 [Intro.md](./Intro.md) 的架構總覽。

## 1. 元件註冊與依賴（第 15–29 行）

```java
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }
```

- 標註 `@Component`，交給 Spring 容器管理（與手動 `new` 的 `JwtAuthenticationFilter` 不同）
- 建構子注入兩個設定值：`app.jwt.secret`（簽章密鑰字串）、`app.jwt.expiration-ms`（token 有效期，毫秒）
- `Keys.hmacShaKeyFor(secret.getBytes(...))` 把字串密鑰轉成 `SecretKey`，代表演算法是 **HMAC-SHA**（HS256/384/512 依 key 長度自動決定），使用的是 **jjwt** 函式庫（`io.jsonwebtoken`）
- `CLAIM_EMAIL` 常數集中管理自訂 claim 名稱，避免字串散落各處（符合 CLAUDE.md 第 6 節「使用具名常數，不要在多個類別中散落原始參數名稱」的原則）

## 2. 簽發 Token：`generateToken`（第 31–42 行）

```java
public String generateToken(String userId, String email) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
            .subject(userId)
            .claim(CLAIM_EMAIL, email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact();
}
```

- `subject`：放字串 GUID `userId`，JWT 標準欄位，代表這個 token 屬於誰
- `claim(CLAIM_EMAIL, email)`：自訂欄位，額外把 email 帶進 token，讓後續驗證時不用再查資料庫就能拿到 email
- `claim(CLAIM_ROLE, role)`：自訂欄位，保存 `admin` / `user` 角色，供 filter 建立 Spring Security authority
- `issuedAt` / `expiration`：簽發時間與到期時間，到期時間 = 簽發時間 + `expirationMs`
- `signWith(signingKey)`：用 HMAC 密鑰簽章，確保 token 沒有被竄改
- `.compact()`：輸出最終的 JWT 字串（`header.payload.signature` 格式）

**呼叫時機**：`AuthService.register()` / `AuthService.login()` 在密碼用 `BCryptPasswordEncoder` 驗證通過、且透過 SQL Server 拿到使用者資料後呼叫，回傳 `AuthResponse(token, user)` 給前端。

## 3. 驗證與解析 Token：`parseToken`（第 44–58 行）

```java
public Optional<AuthenticatedUser> parseToken(String token) {
    try {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String email = claims.get(CLAIM_EMAIL, String.class);
        String role = claims.get(CLAIM_ROLE, String.class);
        return UserRole.fromValue(role)
                .map(userRole -> new AuthenticatedUser(userId, email, userRole.value()));
    } catch (JwtException | IllegalArgumentException ex) {
        return Optional.empty();
    }
}
```

- `Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)`：用同一把簽章密鑰驗證 token 的簽章與格式，並解析出 payload（`Claims`）
- 從 claims 還原字串 GUID `userId`（`subject`）、`email` 與 `role`（自訂 claim），組成 `AuthenticatedUser(userId, email, role)` record 回傳
- **關鍵設計**：回傳型別是 `Optional<AuthenticatedUser>` 而不是直接回傳 `Claims` 或拋例外。任何驗證失敗的情境（簽章不符、token 過期、格式錯誤 → `JwtException`；subject 缺失或非法 → `IllegalArgumentException`）都被 `catch` 統一吞下，回傳 `Optional.empty()`
- 這樣設計的好處：呼叫端（`JwtAuthenticationFilter`）不需要處理例外，只要用 `Optional` 鏈式操作（`flatMap` / `ifPresent`）即可，也避免把 JWT 函式庫的例外細節（例如過期訊息）意外洩漏到上層或回應給前端

## 密鑰與設定管理

- 正式環境的 `app.jwt.secret` 應以環境變數 `JWT_SECRET` 覆蓋，不應依賴 `application.yml` 內建的預設值（詳見 [SecurityConfig.md](./SecurityConfig.md) 及 [Intro.md](./Intro.md) 的「密碼與密鑰」段落）
- `expirationMs` 目前設定約 24 小時（`86400000`），沒有 refresh token 機制，token 過期後使用者需要重新登入

## 與其他元件的關係

```text
AuthService --generateToken(userId, email, role)--> JwtTokenProvider --> JWT 字串 --> 回給前端
JwtAuthenticationFilter --parseToken(token)--> JwtTokenProvider --> Optional<AuthenticatedUser> --> 寫入 SecurityContext
```

`JwtTokenProvider` 本身不知道 Spring Security 的存在（沒有依賴 `SecurityContextHolder` 等），單純是一個「JWT 讀寫工具」，職責單一，符合關注點分離。
