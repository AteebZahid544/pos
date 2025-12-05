package com.example.pos.DTO;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String email;
    private String otp;
}
