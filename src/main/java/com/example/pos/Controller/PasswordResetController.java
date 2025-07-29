package com.example.pos.Controller;

import com.example.pos.DTO.ChangePasswordRequest;
import com.example.pos.DTO.OtpVerificationRequest;
import com.example.pos.DTO.ResetPasswordRequest;
import com.example.pos.Service.PasswordResetService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/send-otp")
    public Status sendOtp(@RequestBody ResetPasswordRequest request) {
        return passwordResetService.sendOtp(request.getEmail());
    }

    @PostMapping("/verify-otp")
    public Status verifyOtp(@RequestBody OtpVerificationRequest request) {
        return passwordResetService.verifyOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/reset")
    public Status resetPassword(@RequestBody ChangePasswordRequest request) {
        return passwordResetService.resetPassword(request);
    }
}

