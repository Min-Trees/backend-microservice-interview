package com.auth.service.controller;

import com.auth.service.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtService jwtService;

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> jwks() {
        return jwtService.getJwks();
    }
}
