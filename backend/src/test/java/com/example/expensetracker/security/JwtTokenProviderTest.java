package com.example.expensetracker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "1234567890123456789012345678901234567890123456789012345678901234";
    private static final String USER_ID = "A1B2C3D4E5F60718293A4B5C6D7E8F90";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 86_400_000);

    @Test
    void parsesTokenWithGuidSubject() {
        String token = jwtTokenProvider.generateToken(USER_ID, "jane@example.com", "admin");

        Optional<AuthenticatedUser> user = jwtTokenProvider.parseToken(token);

        assertThat(user).contains(new AuthenticatedUser(USER_ID, "jane@example.com", "admin"));
    }

    @Test
    void rejectsTokenWithNonGuidSubject() {
        String token = jwtTokenProvider.generateToken("42", "jane@example.com", "user");

        Optional<AuthenticatedUser> user = jwtTokenProvider.parseToken(token);

        assertThat(user).isEmpty();
    }

    @Test
    void rejectsTokenWithUnsupportedRole() {
        String token = Jwts.builder()
                .subject(USER_ID)
                .claim("email", "jane@example.com")
                .claim("role", "guest")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Optional<AuthenticatedUser> user = jwtTokenProvider.parseToken(token);

        assertThat(user).isEmpty();
    }
}
