package com.auth.service.controller;

import com.auth.service.dto.request.LoginRequest;
import com.auth.service.dto.request.UserRequest;
import com.auth.service.dto.request.VerifyRequest;
import com.auth.service.dto.response.AuthResponse;
import com.auth.service.dto.response.UserResponse;
import com.auth.service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody UserRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify")
    public void verify(@Valid @RequestBody VerifyRequest request) {
        authService.verify(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
