package org.enterpriseauditing.enterpriseauditing.service;

import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.dto.AuthResponse;
import org.enterpriseauditing.enterpriseauditing.dto.LoginRequest;
import org.enterpriseauditing.enterpriseauditing.dto.RegisterRequest;
import org.enterpriseauditing.enterpriseauditing.model.AppUser;
import org.enterpriseauditing.enterpriseauditing.model.Role;
import org.enterpriseauditing.enterpriseauditing.repository.AppUserRepository;
import org.enterpriseauditing.enterpriseauditing.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public AppUser register(RegisterRequest request) {

        if (appUserRepository.existsByUsername(request.username())) {
            throw new RuntimeException(
                    "Username already exists: " + request.username()
            );
        }

        AppUser user = AppUser.builder()
                .id(UUID.randomUUID().toString())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return appUserRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        String username = request.username();

        if (loginAttemptService.isBlocked(username)) {
            throw new RuntimeException(
                    "Too many failed login attempts. Please try again later."
            );
        }

        AppUser user = appUserRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid username or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            loginAttemptService.recordFailedAttempt(username);

            throw new RuntimeException(
                    "Invalid username or password"
            );
        }

        // Successful login → clear failed attempts
        loginAttemptService.resetAttempts(username);

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole()
        );

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRole()
        );
    }
}