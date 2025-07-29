package com.example.pos.Service;

import com.example.pos.DTO.AuthenticationDto;
import com.example.pos.entity.AdminDatabase;
import com.example.pos.entity.Authentication;
import com.example.pos.entity.Session;
import com.example.pos.repo.AdminDatabaseRepository;
import com.example.pos.repo.AuthenticationRepo;

import com.example.pos.repo.SessionRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {
    private static final int SESSION_EXPIRY_MINUTES = 10;

    @Autowired
    private AuthenticationRepo authenticationRepo;

    @Autowired
    private SessionRepo sessionRepo;
    @Autowired
    private EmailService emailService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AdminDatabaseRepository adminDatabaseRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Status register(AuthenticationDto user) {
        if (user.getUsername() == null || user.getPassword() == null || user.getEmail() == null|| user.getPhoneNumber() == null) {
            return new Status(StatusMessage.FAILURE, "Username, password, email and phone number cannot be null");
        }
        Optional<Authentication> authentication = authenticationRepo.findByPhoneNumber(user.getPhoneNumber());

        if (authentication.isPresent()) {
            if (authentication.get().getPhoneNumber().equals(user.getPhoneNumber()) ||
                    authentication.get().getEmail().equals(user.getEmail())) {
                return new Status(StatusMessage.FAILURE, "User already registered with these credentials");
            }
        }


        // Save user in central authentication DB
        Authentication registerUser = new Authentication();
        registerUser.setUsername(user.getUsername());
        registerUser.setPhoneNumber(user.getPhoneNumber());
        registerUser.setPassword(passwordEncoder.encode(user.getPassword()));
        registerUser.setEmail(user.getEmail());
        registerUser.setIsActive(true);
        authenticationRepo.save(registerUser);

        // Construct DB name
        String dbName = "pos_user_" + user.getUsername().toLowerCase().replaceAll("[^a-z0-9]", "_");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Step 1: Create new database
            stmt.executeUpdate("CREATE DATABASE " + dbName);

            // Step 2: Save to admin_databases table
            AdminDatabase dbTrack = new AdminDatabase();
            dbTrack.setUsername(user.getUsername());
            dbTrack.setDatabaseName(dbName);
            dbTrack.setStatus("ACTIVE");
            dbTrack.setPhoneNumber(user.getPhoneNumber());
            dbTrack.setCreatedAt(LocalDateTime.now());
            adminDatabaseRepo.save(dbTrack);


            try (Connection newDbConn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/" + dbName,
                    "root", "12345")) {

                try {
                    Resource resource = new ClassPathResource("sql/init.sql");
                    String sqlScript = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                    Statement newDbStmt = newDbConn.createStatement();
                    for (String sql : sqlScript.split(";")) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            newDbStmt.execute(sql);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return new Status(StatusMessage.FAILURE, "Failed to read init.sql file: " + e.getMessage());
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
            return new Status(StatusMessage.FAILURE, "User saved but DB/table creation failed: " + e.getMessage());
        }
        emailService.sendRegisterNotification(user.getEmail(), user.getUsername());
        return new Status(StatusMessage.SUCCESS, "User registered");
    }

    @Transactional
    public Status login(String phoneNumber, String password) {
        Optional<Authentication> authOpt = authenticationRepo.findByPhoneNumber(phoneNumber);

        if (authOpt.isEmpty() || !authOpt.get().getIsActive()) {
            return new Status(StatusMessage.FAILURE, "Invalid credentials or inactive account");
        }

        Authentication auth = authOpt.get();

        // ✅ Match raw password with hashed password
        if (!passwordEncoder.matches(password, auth.getPassword())) {
            return new Status(StatusMessage.FAILURE, "Invalid credentials");
        }

        // ✅ Create new session
        Session session = new Session();
        session.setPhoneNumber(phoneNumber);
        session.setToken(UUID.randomUUID().toString());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));
        session.setIsActive(true);
        sessionRepo.save(session);

        // ✅ Fetch email from Authentication table
        String email = auth.getEmail();
        String username = auth.getUsername();

        // ✅ Send email
        emailService.sendLoginNotification(email, username);

        return new Status(StatusMessage.SUCCESS, "User logged in successfully");
    }

    public Status validateSession(String token) {
        Optional<Session> sessionOpt = sessionRepo.findByToken(token);

        if (sessionOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Invalid token");
        }

        Session session = sessionOpt.get();

        if (!session.isActive() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setIsActive(false);
            sessionRepo.save(session);
            return new Status(StatusMessage.FAILURE, "Session expired");
        }

        // Extend session on activity
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));
        sessionRepo.save(session);

        return new Status(StatusMessage.SUCCESS, "Session active and extended");
    }
}
