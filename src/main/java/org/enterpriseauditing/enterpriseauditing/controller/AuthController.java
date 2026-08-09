package org.enterpriseauditing.enterpriseauditing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.dto.AuthResponse;
import org.enterpriseauditing.enterpriseauditing.dto.LoginRequest;
import org.enterpriseauditing.enterpriseauditing.dto.RegisterRequest;
import org.enterpriseauditing.enterpriseauditing.model.AppUser;
import org.enterpriseauditing.enterpriseauditing.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AppUser register(
            @Valid @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}