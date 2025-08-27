package com.auth.service.controller;

import com.auth.service.dto.UserDto;
import com.auth.service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/users/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        return authService.getUserById(id);
    }
}
