package com.abc.user_service.service;

import com.abc.user_service.dto.request.LoginRequest;
import com.abc.user_service.dto.request.UserRequest;
import com.abc.user_service.dto.request.VerifyRequest;
import com.abc.user_service.dto.response.AuthResponse;
import com.abc.user_service.dto.response.UserResponse;
import com.abc.user_service.entity.User;
import com.abc.user_service.entity.UserStatus;
import com.abc.user_service.mapper.UserMapper;
import com.abc.user_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;

    @Transactional
    public UserResponse register(UserRequest request) {
        Optional<User> existing = userRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());
        user.setVerificationCode(UUID.randomUUID().toString());
        User saved = userRepository.save(user);
        sendVerificationEmail(saved);
        return userMapper.toResponse(saved);
    }

    public void verify(VerifyRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!request.getCode().equals(user.getVerificationCode())) {
            throw new RuntimeException("Invalid verification code");
        }
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
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken);
    }

    private void sendVerificationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Verify your account");
            message.setText("Your verification code: " + user.getVerificationCode());
            mailSender.send(message);
        } catch (Exception e) {
            // Log and continue
        }
    }
}
