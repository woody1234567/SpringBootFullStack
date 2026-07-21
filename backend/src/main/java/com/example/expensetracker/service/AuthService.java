package com.example.expensetracker.service;

import com.example.expensetracker.constant.ResultCode;
import com.example.expensetracker.dto.request.LoginRequest;
import com.example.expensetracker.dto.request.RegisterRequest;
import com.example.expensetracker.dto.response.AuthResponse;
import com.example.expensetracker.dto.response.UserResponse;
import com.example.expensetracker.exception.DuplicateResourceException;
import com.example.expensetracker.repository.UserRepository;
import com.example.expensetracker.repository.model.CreateUserResult;
import com.example.expensetracker.repository.model.FindUserResult;
import com.example.expensetracker.repository.model.UserRow;
import com.example.expensetracker.security.JwtTokenProvider;
import com.example.expensetracker.security.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        CreateUserResult result = userRepository.createUser(request.email(), passwordHash, request.displayName());

        if (ResultCode.DUPLICATE.equals(result.resultCode())) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (!ResultCode.SUCCESS.equals(result.resultCode())) {
            log.error("app_user.SP_CREATE_USER returned result_code={}", result.resultCode());
            throw new IllegalStateException("Unable to complete the operation");
        }

        String role = UserRole.USER.value();
        String token = jwtTokenProvider.generateToken(result.userId(), request.email(), role);
        UserResponse user = new UserResponse(result.userId(), request.email(), request.displayName(), role);
        return new AuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        FindUserResult result = userRepository.findByEmail(request.email());

        UserRow userRow = result.user()
                .filter(u -> passwordEncoder.matches(request.password(), u.passwordHash()))
                .filter(UserRow::active)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String role = UserRole.require(userRow.role()).value();
        String token = jwtTokenProvider.generateToken(userRow.userId(), userRow.email(), role);
        UserResponse user = new UserResponse(userRow.userId(), userRow.email(), userRow.displayName(), role);
        return new AuthResponse(token, user);
    }
}
