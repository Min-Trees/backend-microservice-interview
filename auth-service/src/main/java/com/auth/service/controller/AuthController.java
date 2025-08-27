package com.auth.service.controller;

import com.auth.service.dto.request.LoginRequest;
import com.auth.service.dto.request.RefreshRequest;
import com.auth.service.dto.request.UserRequest;
import com.auth.service.dto.response.AuthResponse;
import com.auth.service.dto.response.UserResponse;
import com.auth.service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody UserRequest request) {
        return authService.register(request);
    }

    @GetMapping("/verify")
    public void verify(@RequestParam("token") String token) {
        authService.verify(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
    }
}
