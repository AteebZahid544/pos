package com.example.pos.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

//    @Value("${twilio.accountSid}")
//    private String accountSid;
//
//    @Value("${twilio.authToken}")
//    private String authToken;
//
//    @Value("${twilio.phoneNumber}")
//    private String twilioPhoneNumber;

//    @PostConstruct
//    public void initTwilio() {
//        Twilio.init(accountSid, authToken);
//    }

    public void sendLoginNotification(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("AZ Management System Login Alert");
        message.setText("Dear " + username + ",\n\nYou have successfully logged into the AZ Management system."
                + "\n\nIf this wasn't you, please contact the administrator immediately."
                + "\n\nRegards,\nAZ Management System");
        mailSender.send(message);
    }

    public void sendOtpNotification(String toEmail, String username, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("AZ Management System OTP Verification");
        message.setText("Dear " + username + ",\n\nYour OTP for AZ Management system verification is: " + otp
                + "\n\nPlease use this OTP to complete your login process."
                + "\n\nRegards,\nAZ Management System");
        mailSender.send(message);
    }

    public void sendRegisterNotification(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("AZ Management System OTP Verification");
        message.setText("Dear " + username + ",\n\nYou have successfully logged into the AZ Management system."
                + "\n\nIf this wasn't you, please contact the administrator immediately."
                + "\n\nRegards,\nAZ Management System");
        mailSender.send(message);
    }

//    public void sendOtpSms(String phoneNumber, String otp) {
//        // Send via SMS gateway like Twilio, Nexmo, etc.
//        String message = "Your OTP is: " + otp + ". Valid for 10 minutes.";
//
//        // Example with Twilio:
//
//        Message.creator(
//                new PhoneNumber(phoneNumber),
//                new PhoneNumber(twilioPhoneNumber),
//                message
//        ).create();
//
//
//        // Placeholder for now:
//        System.out.println("Sending SMS to " + phoneNumber + ": " + message);
//    }

}
