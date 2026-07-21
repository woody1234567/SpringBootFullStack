# JwtAuthenticationFilter 逐項解析

檔案：`backend/src/main/java/com/example/expensetracker/security/JwtAuthenticationFilter.java`

這是每個 HTTP 請求進來後，負責「從 JWT 還原身份、寫入 Spring Security Context」的過濾器。與 [SecurityConfig.md](./SecurityConfig.md) 中提到的 `addFilterBefore(...)` 對應，也是 [Intro.md](./Intro.md) 整體流程圖裡「之後每個請求」的實作細節。

## 1. 繼承 `OncePerRequestFilter`（第 17 行）

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

保證同一個請求在一次 filter chain 中只會被執行一次（避免 forward/include 造成重複驗證），是 Spring 官方推薦的 HTTP filter 基底類別。

## 2. 建構子注入（第 21–25 行）

```java
private final JwtTokenProvider jwtTokenProvider;

public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
}
```

這個 filter 是在 `SecurityConfig` 裡用 `new JwtAuthenticationFilter(jwtTokenProvider)` 手動建立、透過 `addFilterBefore` 插入 filter chain（不是 `@Component` 交給 Spring 容器管理），所以依賴用建構子參數傳入即可，不需要 `@Autowired`。

## 3. 核心驗證邏輯 `doFilterInternal`（第 27–42 行）

```java
extractToken(request)
        .flatMap(jwtTokenProvider::parseToken)
        .ifPresent(authenticatedUser -> {
            var authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });

filterChain.doFilter(request, response);
```

流程是一串 `Optional` 鏈式操作：

1. `extractToken(request)` → `Optional<String>`，從 header 拿出 raw token 字串
2. `.flatMap(jwtTokenProvider::parseToken)` → 呼叫 `JwtTokenProvider.parseToken(token)`，內部驗證簽章、解析 claims，成功則回傳 `Optional<AuthenticatedUser>`（`AuthenticatedUser` 是包含 `userId`、`email`、`role` 的 record），失敗（過期、簽章不符、格式錯誤）則回傳 `Optional.empty()`
3. `.ifPresent(...)` → 只有在成功解析出使用者時才執行：
   - 建立 `UsernamePasswordAuthenticationToken`，把 `AuthenticatedUser` 當作 principal，credentials 給 `null`（JWT 場景不需要密碼），authorities 依 role 映射為 `ROLE_ADMIN` 或 `ROLE_USER`
   - 寫入 `SecurityContextHolder`，代表這個 request 的上下文中「已通過驗證」

**關鍵設計**：無論 token 有沒有、解析成不成功，最後都會呼叫 `filterChain.doFilter(request, response)` 放行到下一個 filter，本身**不會**直接擋下或回傳錯誤。真正擋下未驗證請求的是後面 `SecurityConfig` 裡的 `authorizeHttpRequests(...).anyRequest().hasAnyRole("ADMIN", "USER")` 規則 —— 如果 `SecurityContext` 裡沒有 authentication，Spring Security 會觸發 `authenticationEntryPoint`（即 `SecurityConfig.handleUnauthenticated`），回傳統一格式的 401 JSON。

這種「filter 只負責認證，交給 authorizeHttpRequests 決定要不要放行」的分工，讓 `/api/auth/**` 這種 `permitAll()` 的端點即使帶著壞掉的 token 也不會被擋，設計上較有彈性。

## 4. Token 擷取 `extractToken`（第 44–50 行）

```java
private Optional<String> extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
        return Optional.of(header.substring(BEARER_PREFIX.length()));
    }
    return Optional.empty();
}
```

- 只接受標準的 `Authorization: Bearer <token>` 格式（`BEARER_PREFIX = "Bearer "`）
- 沒有這個 header，或前綴不是 `Bearer `，一律視為沒有 token，回傳 `Optional.empty()`，不會拋例外

## 目前設計上的限制 / 可觀察點

- **角色來源**：角色由登入/註冊流程寫入 JWT `role` claim，filter 驗證後動態組出對應的 `GrantedAuthority` 清單；目前支援 `admin` / `user`
- **沒有例外處理的分支**：所有失敗情境（token 不存在、格式錯、過期、簽章錯）都被 `Optional.empty()` 統一吸收，過濾器本身不會記錄失敗原因；若未來要做安全稽核（例如記錄可疑的偽造 token 嘗試），需要額外在 `parseToken` 或這裡加上 log
