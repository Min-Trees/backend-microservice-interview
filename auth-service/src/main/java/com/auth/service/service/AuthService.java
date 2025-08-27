package com.auth.service.service;

import com.auth.service.client.UserClient;
import com.auth.service.dto.LoginRequest;
import com.auth.service.dto.RefreshRequest;
import com.auth.service.dto.TokenResponse;
import com.auth.service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserClient userClient;
    private final JwtService jwtService;

    public UserDto getUserById(Long id) {
        return userClient.getUserById(id);
    }

    public TokenResponse login(LoginRequest request) {
        UserDto user = userClient.login(request);
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        return new TokenResponse(access, refresh);
    }

    public TokenResponse refresh(RefreshRequest request) {
        String token = request.getRefreshToken();
        Long userId = Long.valueOf(jwtService.parse(token).getSubject());
        UserDto user = userClient.getUserById(userId);
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        return new TokenResponse(access, refresh);
    }
}
