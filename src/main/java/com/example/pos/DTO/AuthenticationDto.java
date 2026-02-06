package com.example.pos.DTO;

import lombok.Data;

@Data
public class AuthenticationDto {
    private String username;
    private String password;
    private String phoneNumber;
    private boolean isActive;
    private String email;
}
