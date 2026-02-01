package com.example.pos.Service;

import com.example.pos.DTO.AuthenticationDto;
import com.example.pos.DTO.LoginDto;
import com.example.pos.DTO.LoginResponseDto;
import com.example.pos.config.CurrentTenantIdentifierResolverImpl;
import com.example.pos.config.MultiTenantConnectionProviderImpl;
import com.example.pos.config.TenantContext;
import com.example.pos.entity.central.*;
import com.example.pos.repo.central.AdminDatabaseRepository;
import com.example.pos.repo.central.AuthenticationRepo;

import com.example.pos.repo.central.EmployeeLoginRepo;
import com.example.pos.repo.central.SessionRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {
    private static final int SESSION_EXPIRY_MINUTES = 20;

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

    @Autowired
    private DataSource centralDataSource; // central authentication DB

    @Autowired
    private MultiTenantConnectionProviderImpl multiTenantConnectionProvider;

    @Autowired
    private EmployeeLoginRepo employeeLoginRepo;

    public Status register(AuthenticationDto user) {

        // Validate inputs
        if (user.getUsername() == null || user.getPassword() == null ||
                user.getEmail() == null || user.getPhoneNumber() == null) {
            return new Status(StatusMessage.FAILURE,
                    "Username, password, email and phone number cannot be null");
        }

        // ✅ Use central datasource to check existing user
        try (Connection conn = centralDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM authentication WHERE phone_number=? OR email=?")) {
            ps.setString(1, user.getPhoneNumber());
            ps.setString(2, user.getEmail());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Status(StatusMessage.FAILURE, "User already registered");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Status(StatusMessage.FAILURE, "Error checking user: " + e.getMessage());
        }

        // Save user in central authentication DB
        Authentication authUser = new Authentication();
        authUser.setUsername(user.getUsername());
        authUser.setPhoneNumber(user.getPhoneNumber());
        authUser.setEmail(user.getEmail());
        authUser.setPassword(passwordEncoder.encode(user.getPassword()));
        authUser.setActive(true);
        authUser.setRole("OWNER");
        // Construct tenant schema name
        String schemaName = "tenant_" + user.getPhoneNumber().toLowerCase().replaceAll("[^a-z0-9]", "_");
        authUser.setDatabaseName(schemaName);

        authenticationRepo.save(authUser);

        // --------------- Create tenant schema ----------------
        try (Connection conn = centralDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1️⃣ Create schema
            stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS " + schemaName);

            // 2️⃣ Save schema info in admin database
            AdminDatabase dbTrack = new AdminDatabase();
            dbTrack.setUsername(user.getUsername());
            dbTrack.setDatabaseName(schemaName);
            dbTrack.setStatus("ACTIVE");
            dbTrack.setPhoneNumber(user.getPhoneNumber());
            dbTrack.setCreatedAt(LocalDateTime.now());
            adminDatabaseRepo.save(dbTrack);

            // 3️⃣ Initialize tables
            ClassPathResource resource = new ClassPathResource("sql/init.sql");
            String sqlScript = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            for (String sql : sqlScript.split(";")) {
                sql = sql.trim();
                if (!sql.isEmpty()) {
                    sql = sql.replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS " + schemaName + ".");
                    stmt.execute(sql);
                }
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return new Status(StatusMessage.FAILURE,
                    "User saved but tenant schema creation failed: " + e.getMessage());
        }

        emailService.sendRegisterNotification(user.getEmail(), user.getUsername());
        return new Status(StatusMessage.SUCCESS, "User registered successfully");
    }

    // --------------------- LOGIN ---------------------
//    @Transactional
//    public Status login(LoginDto loginDto) {
//        // 1️⃣ Authenticate using central DB
//        Optional<Authentication> authOpt = authenticationRepo.findByPhoneNumber(loginDto.getPhoneNumber());
//        if (authOpt.isEmpty() || !authOpt.get().isActive()) {
//            return new Status(StatusMessage.FAILURE, "Invalid credentials or inactive account");
//        }
//
//        Authentication auth = authOpt.get();
//
//        if (!passwordEncoder.matches(loginDto.getPassword(), auth.getPassword())) {
//            return new Status(StatusMessage.FAILURE, "Invalid credentials");
//        }
//
//        // 2️⃣ Set tenant for this session
//        TenantContext.setTenantId(auth.getDatabaseName());
//
//        // ✅ All JPA operations after this point will use tenant schema
//        // Example: you can load tenant-specific tables here
//
//        // 3️⃣ Create session
//        Session session = new Session();
//        session.setPhoneNumber(auth.getPhoneNumber());
//        session.setToken(UUID.randomUUID().toString());
//        session.setCreatedAt(LocalDateTime.now());
//        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));
//        sessionRepo.save(session);
//
//        // 4️⃣ Prepare response
//        LoginResponseDto response = new LoginResponseDto();
//        response.setToken(session.getToken());
//        response.setCreatedAt(session.getCreatedAt());
//        response.setExpiresAt(session.getExpiresAt());
//        response.setUsername(auth.getUsername());
//        response.setPhoneNumber(auth.getPhoneNumber());
//        response.setEmail(auth.getEmail());
//        response.setTenantId(auth.getDatabaseName());
//
//        emailService.sendLoginNotification(auth.getEmail(), auth.getUsername());
//
//        return new Status(StatusMessage.SUCCESS, response);
//    }

    @Transactional
    public Status login(LoginDto loginDto) {

        // 1️⃣ Central authentication (OWNER OR EMPLOYEE)
        Authentication auth = authenticationRepo
                .findByPhoneNumber(loginDto.getPhoneNumber())
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials"));

        if (!auth.isActive() ||
                !passwordEncoder.matches(loginDto.getPassword(), auth.getPassword())) {
            return new Status(StatusMessage.FAILURE, "Invalid credentials");
        }

        // 2️⃣ Set tenant
        TenantContext.setTenantId(auth.getDatabaseName());

        // 3️⃣ Check if this user is EMPLOYEE
        Optional<EmployeeLogin> empOpt =
                employeeLoginRepo.findByUsername(auth.getUsername());

        List<String> authorities;

        if (empOpt.isPresent()) {
            // ✅ EMPLOYEE LOGIN
            authorities = empOpt.get().getAuthorities()
                    .stream()
                    .map(Authority::getAuthorityName)
                    .toList();
        } else {
            // ✅ OWNER LOGIN (full access)
            authorities = List.of(
                    "HOME",
                    "PURCHASES",
                    "SALES",
                    "CATEGORIES",
                    "VENDORS & CUSTOMERS",
                    "PAY BILLS",
                    "EXPENSES",
                    "EMPLOYEES",
                    "CLIENT REQUIREMENTS",
                    "EMPLOYEE LOGIN",
                    "OWNER",
                    "PRODUCTION",
                    "OWNER PRODUCTION",
                    "OWNER DASHBOARD",
                    "REPORTS"
            );
        }

        // 4️⃣ Create session
        Session session = new Session();
        session.setPhoneNumber(auth.getPhoneNumber());
        session.setToken(UUID.randomUUID().toString());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));
        sessionRepo.save(session);

        // 5️⃣ Response
        LoginResponseDto response = new LoginResponseDto();
        response.setToken(session.getToken());
        response.setUsername(auth.getUsername());
        response.setPhoneNumber(auth.getPhoneNumber());
        response.setEmail(auth.getEmail());
        response.setTenantId(auth.getDatabaseName());
        response.setAuthorities(authorities);

        //emailService.sendLoginNotification(auth.getEmail(), auth.getUsername());

        return new Status(StatusMessage.SUCCESS, response);
    }



    public Status validateSession(String token) {
        Optional<Session> sessionOpt = sessionRepo.findByToken(token);
        if (sessionOpt.isEmpty()) return new Status(StatusMessage.FAILURE, "Invalid token");

        Session session = sessionOpt.get();
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepo.delete(session);
            return new Status(StatusMessage.FAILURE, "Session expired");
        }

        long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getExpiresAt());
        return new Status(StatusMessage.SUCCESS, "Session active", remainingSeconds);
    }
    public Status logout(String token) {
        Optional<Session> sessionOpt = sessionRepo.findByToken(token);

        if (sessionOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Invalid token");
        }
        sessionRepo.delete(sessionOpt.get());

        return new Status(StatusMessage.SUCCESS, "Logged out successfully");
    }

    public Status getUserData(String mobileNumber){
        Optional<Authentication> authentication= authenticationRepo.findByPhoneNumber(mobileNumber);

        if(authentication.isPresent()){
            Authentication authentication1= authentication.get();

            if (authentication1.getRole().equals("EMPLOYEE")){
                Optional<Authentication>authentication2= authenticationRepo.findByDatabaseNameAndRole(authentication1.getDatabaseName(),"OWNER");
                return new Status(StatusMessage.SUCCESS, authentication2);
            }
        }

            return new Status(StatusMessage.SUCCESS,authentication);

    }
}
