package com.example.pos.DTO;

import com.example.pos.entity.central.Authority;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoginResponseDto {
    private String username;
    private String token;
    private String phoneNumber;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String tenantId;
    private List<String> authorities;
}
