package com.example.expensetracker.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "1234567890123456789012345678901234567890123456789012345678901234";
    private static final String USER_ID = "A1B2C3D4E5F60718293A4B5C6D7E8F90";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 86_400_000);

    @Test
    void parsesTokenWithGuidSubject() {
        String token = jwtTokenProvider.generateToken(USER_ID, "jane@example.com");

        Optional<AuthenticatedUser> user = jwtTokenProvider.parseToken(token);

        assertThat(user).contains(new AuthenticatedUser(USER_ID, "jane@example.com"));
    }

    @Test
    void rejectsTokenWithNonGuidSubject() {
        String token = jwtTokenProvider.generateToken("42", "jane@example.com");

        Optional<AuthenticatedUser> user = jwtTokenProvider.parseToken(token);

        assertThat(user).isEmpty();
    }
}
