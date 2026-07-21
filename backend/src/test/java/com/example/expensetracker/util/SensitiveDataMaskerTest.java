package com.example.expensetracker.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    private record LoginResult(String token, String userId) {
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
        LoginResult result = new LoginResult(
                "eyJhbGciOiJIUzI1NiJ9.payload.sig",
                "A1B2C3D4E5F60718293A4B5C6D7E8F90");

        String described = SensitiveDataMasker.describeResult(result);

        assertThat(described)
                .contains("token=***")
                .contains("userId=A1B2C3D4E5F60718293A4B5C6D7E8F90")
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
