package com.example.expensetracker.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "1234567890123456789012345678901234567890123456789012345678901234";
    private static final String USER_ID = "A1B2C3D4E5F60718293A4B5C6D7E8F90";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 86_400_000);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAdminAuthorityFromTokenRole() throws Exception {
        Authentication authentication = authenticateWithRole("admin");

        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUser(USER_ID, "jane@example.com", "admin"));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void createsUserAuthorityFromTokenRole() throws Exception {
        Authentication authentication = authenticateWithRole("user");

        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUser(USER_ID, "jane@example.com", "user"));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    private Authentication authenticateWithRole(String role) throws Exception {
        String token = jwtTokenProvider.generateToken(USER_ID, "jane@example.com", role);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> capturedAuthentication = new AtomicReference<>();

        filter.doFilter(request, response, (ServletRequest servletRequest, ServletResponse servletResponse) -> {
            capturedAuthentication.set(SecurityContextHolder.getContext().getAuthentication());
        });

        return capturedAuthentication.get();
    }
}
