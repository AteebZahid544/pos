package com.example.pos.Service;

import com.example.pos.DTO.ChangePasswordRequest;
import com.example.pos.entity.Authentication;
import com.example.pos.entity.OtpRequestLog;
import com.example.pos.entity.PasswordResetOtp;
import com.example.pos.repo.AuthenticationRepo;
import com.example.pos.repo.OtpRequestLogRepository;
import com.example.pos.repo.PasswordResetOtpRepository;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetOtpRepository otpRepo;

    @Autowired
    private AuthenticationRepo authRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpRequestLogRepository otpRequestLogRepo;

    @Transactional
    public Status sendOtp(String email) {
        Optional<Authentication> user = authRepo.findByEmail(email);
        if (user.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "User not found");
        }

        // Check request count in the last 1 minute
        LocalDateTime now = LocalDateTime.now();
        int requestCount = otpRequestLogRepo.countRecentRequests(email, now.minusMinutes(1));

        if (requestCount >= 3) {
            // Check when last request was made
            Optional<OtpRequestLog> lastRequest = otpRequestLogRepo.findTopByEmailOrderByRequestTimeDesc(user.get().getEmail());
            if (lastRequest.isPresent()) {
                Duration sinceLast = Duration.between(lastRequest.get().getRequestTime(), now);
                if (sinceLast.toMinutes() < 5) {
                    return new Status(StatusMessage.FAILURE, "Too many attempts. Try again after 5 minutes.");
                }
            }
        }

        // ✅ Proceed to send OTP
        String otp = String.format("%04d", new Random().nextInt(10000));
        PasswordResetOtp resetOtp = new PasswordResetOtp();
        resetOtp.setEmail(user.get().getEmail());
        resetOtp.setOtp(otp);
        resetOtp.setExpirationTime(now.plusMinutes(10));

        otpRepo.deleteByEmail(user.get().getEmail());
        otpRepo.save(resetOtp);

        // ✅ Save request log
        OtpRequestLog log = new OtpRequestLog();
        log.setEmail(user.get().getEmail());
        log.setRequestTime(now);
        otpRequestLogRepo.save(log);

        Authentication authentication=user.get();

        // Send OTP via email
        emailService.sendOtpNotification(authentication.getEmail(), authentication.getUsername(), otp);

//        // ✅ Send OTP via SMS (if phone number exists)
//        if (authentication.getPhoneNumber() != null && !authentication.getPhoneNumber().isEmpty()) {
//            String formattedPhone = formatToE164(authentication.getPhoneNumber());
//            emailService.sendOtpSms(formattedPhone, otp);
//        }
        return new Status(StatusMessage.SUCCESS, "OTP sent successfully");
    }

    public Status verifyOtp(String email, String otp) {
        Optional<PasswordResetOtp> record = otpRepo.findByEmailAndOtp(email, otp);
        if (record.isEmpty() || record.get().getExpirationTime().isBefore(LocalDateTime.now())) {
            return new Status(StatusMessage.FAILURE, "Invalid or expired OTP");
        }

        return new Status(StatusMessage.SUCCESS, "OTP verified successfully");
    }

    @Transactional
    public Status resetPassword(ChangePasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty() ||request.getConfirmPassword() == null|| request.getEmail() == null || request.getEmail().isEmpty() ) {
            return new Status(StatusMessage.FAILURE, "Some fields are empty");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new Status(StatusMessage.FAILURE, "Passwords do not match");
        }

        Optional<Authentication> user = authRepo.findByEmail(request.getEmail());
        if (user.isPresent()) {
            Authentication auth = user.get();
            auth.setPassword(passwordEncoder.encode(request.getNewPassword()));
            authRepo.save(auth);
            otpRepo.deleteByEmail(request.getEmail());
            return new Status(StatusMessage.SUCCESS, "Password updated successfully");
        }

        return new Status(StatusMessage.FAILURE, "User not found");
    }

    private String formatToE164(String localPhoneNumber) {
        if (localPhoneNumber.startsWith("0")) {
            return "+92" + localPhoneNumber.substring(1);
        } else if (!localPhoneNumber.startsWith("+")) {
            return "+92" + localPhoneNumber;
        }
        return localPhoneNumber;
    }

}

