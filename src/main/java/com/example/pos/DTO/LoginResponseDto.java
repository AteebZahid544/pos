package com.example.pos.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginResponseDto {
    private String username;
    private String token;
    private String phoneNumber;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String tenantId;
}
