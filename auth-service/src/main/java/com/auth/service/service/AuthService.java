package com.auth.service.service;

import com.auth.service.client.UserClient;
import com.auth.service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserClient userClient;

    public UserDto getUserById(Long id) {
        return userClient.getUserById(id);
    }
}
