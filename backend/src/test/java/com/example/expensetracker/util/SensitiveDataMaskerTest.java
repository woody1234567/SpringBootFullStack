package com.example.expensetracker.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    private record LoginResult(String token, Long userId) {
    }

    @Test
    void masksArgumentNamedPassword() {
        String described = SensitiveDataMasker.describeArgs(
                new String[] {"password"}, new Object[] {"Password123"});

        assertThat(described).isEqualTo("password=***");
    }

    @Test
    void partiallyMasksArgumentNamedEmail() {
        String described = SensitiveDataMasker.describeArgs(
                new String[] {"email"}, new Object[] {"jane@example.com"});

        assertThat(described).isEqualTo("email=j***@example.com");
    }

    @Test
    void masksSensitiveRecordComponentButKeepsSiblingsVisible() {
        LoginResult result = new LoginResult("eyJhbGciOiJIUzI1NiJ9.payload.sig", 42L);

        String described = SensitiveDataMasker.describeResult(result);

        assertThat(described)
                .contains("token=***")
                .contains("userId=42")
                .doesNotContain("eyJhbGciOiJIUzI1NiJ9");
    }

    @Test
    void describesCollectionsBySizeOnly() {
        String described = SensitiveDataMasker.describeResult(List.of("a", "b", "c"));

        assertThat(described).matches(".*\\[size=3]$");
    }

    @Test
    void describesNullWithoutThrowing() {
        assertThat(SensitiveDataMasker.describeResult(null)).isEqualTo("null");
    }
}
