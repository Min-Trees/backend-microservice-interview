package com.auth.service.dto.response;

import com.auth.service.entity.EloRank;
import com.auth.service.entity.UserStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private Long roleId;
    private String email;
    private String fullName;
    private LocalDate dateOfBirth;
    private String address;
    private UserStatus status;
    private Boolean isStudying;
    private Integer eloScore;
    private EloRank eloRank;
    private LocalDateTime createdAt;
}
