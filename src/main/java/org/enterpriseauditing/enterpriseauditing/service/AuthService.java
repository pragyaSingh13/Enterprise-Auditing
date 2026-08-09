package org.enterpriseauditing.enterpriseauditing.service;

import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.dto.AuthResponse;
import org.enterpriseauditing.enterpriseauditing.dto.LoginRequest;
import org.enterpriseauditing.enterpriseauditing.dto.RegisterRequest;
import org.enterpriseauditing.enterpriseauditing.model.AppUser;
import org.enterpriseauditing.enterpriseauditing.model.Role;
import org.enterpriseauditing.enterpriseauditing.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

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
                .build();


        user.setRole(Role.USER);

        return appUserRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        AppUser user = appUserRepository
                .findByUsername(request.username())
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password")
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid username or password"
            );
        }

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