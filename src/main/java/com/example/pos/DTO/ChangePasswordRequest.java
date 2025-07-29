package com.example.pos.DTO;

public class ChangePasswordRequest {
    private String email;
    private String newPassword;
    private String confirmPassword;
//    private String otp;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
//    public String getOtp() {
//        return otp;
//    }
//    public void setOtp(String otp) {
//        this.otp = otp;
//    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}