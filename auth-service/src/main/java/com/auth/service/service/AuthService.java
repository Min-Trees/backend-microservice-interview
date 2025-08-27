package com.auth.service.service;

import com.auth.service.dto.request.LoginRequest;
import com.auth.service.dto.request.UserRequest;
import com.auth.service.dto.response.AuthResponse;
import com.auth.service.dto.response.UserResponse;
import com.auth.service.entity.RefreshToken;
import com.auth.service.entity.Role;
import com.auth.service.entity.User;
import com.auth.service.entity.UserStatus;
import com.auth.service.repository.RefreshTokenRepository;
import com.auth.service.client.UserClient;
import com.auth.service.dto.request.UserSyncRequest;
import com.auth.service.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserClient userClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;

    @Transactional
    public UserResponse register(UserRequest request) {
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        Role role = new Role();
        role.setId(request.getRoleId());
        user.setRole(role);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setIsStudying(request.getIsStudying());
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());
        user.setVerificationCode(UUID.randomUUID().toString());
        User saved = userRepository.save(user);
        userClient.createUser(new UserSyncRequest(saved.getId(), saved.getEmail()));
        sendVerificationEmail(saved);
        return toUserResponse(saved);
    }

    public void verify(String token) {
        User user = userRepository.findByVerificationCode(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        user.setStatus(UserStatus.VERIFIED);
        user.setVerificationCode(null);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (user.getStatus() != UserStatus.VERIFIED) {
            throw new RuntimeException("Email not verified");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        String accessToken = jwtService.generateAccessToken(user);
        JwtService.TokenInfo refresh = jwtService.generateRefreshToken(user);
        storeRefreshToken(user, refresh);
        return new AuthResponse(accessToken, refresh.token(), "Bearer", jwtService.getAccessTokenTtl());
    }

    public AuthResponse refresh(String refreshToken) {
        JwtService.TokenInfo info = jwtService.parseToken(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByJti(info.jti())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Invalid refresh token");
        }
        if (!stored.getTokenHash().equals(hashToken(refreshToken))) {
            throw new RuntimeException("Invalid refresh token" );
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String accessToken = jwtService.generateAccessToken(user);
        JwtService.TokenInfo newRefresh = jwtService.generateRefreshToken(user);
        storeRefreshToken(user, newRefresh);
        return new AuthResponse(accessToken, newRefresh.token(), "Bearer", jwtService.getAccessTokenTtl());
    }

    public void logout(String refreshToken) {
        JwtService.TokenInfo info = jwtService.parseToken(refreshToken);
        refreshTokenRepository.findByJti(info.jti()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private void storeRefreshToken(User user, JwtService.TokenInfo info) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setJti(info.jti());
        entity.setTokenHash(hashToken(info.token()));
        entity.setExpiresAt(info.expiresAt());
        refreshTokenRepository.save(entity);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to hash token", e);
        }
    }

    private void sendVerificationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Verify your account");
            message.setText("Your verification code: " + user.getVerificationCode());
            mailSender.send(message);
        } catch (Exception e) {
            // log and ignore
        }
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        if (user.getRole() != null) {
            response.setRoleId(user.getRole().getId());
        }
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setAddress(user.getAddress());
        response.setStatus(user.getStatus());
        response.setIsStudying(user.getIsStudying());
        response.setEloScore(user.getEloScore());
        response.setEloRank(user.getEloRank());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
