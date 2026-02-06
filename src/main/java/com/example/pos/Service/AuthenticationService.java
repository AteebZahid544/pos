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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        try (Connection conn = centralDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM authentication WHERE (phone_number=? AND email=? AND username=?) AND is_active=true")) {
            ps.setString(1, user.getPhoneNumber());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getUsername());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // ADD DEBUG LOGGING HERE
                System.out.println("DEBUG - Found existing user:");
                System.out.println("  Username: " + rs.getString("username"));
                System.out.println("  Email: " + rs.getString("email"));
                System.out.println("  Phone: " + rs.getString("phone_number"));
                System.out.println("  is_active: " + rs.getBoolean("is_active"));
                System.out.println("  Role: " + rs.getString("role"));

                String existingEmail = rs.getString("email");
                String existingPhone = rs.getString("phone_number");

                if (existingEmail != null && existingEmail.equals(user.getEmail())) {
                    return new Status(StatusMessage.FAILURE, "Email already registered");
                } else if (existingPhone != null && existingPhone.equals(user.getPhoneNumber())) {
                    return new Status(StatusMessage.FAILURE, "Phone number already registered");
                } else {
                    return new Status(StatusMessage.FAILURE, "User already registered");
                }
            } else {
                System.out.println("DEBUG - No active user found with these credentials");
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
            String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            stmt.executeUpdate(createSchemaSQL);

            System.out.println("✅ Schema created: " + schemaName);

            // 2️⃣ Save schema info in admin database
            AdminDatabase dbTrack = new AdminDatabase();
            dbTrack.setUsername(user.getUsername());
            dbTrack.setDatabaseName(schemaName);
            dbTrack.setStatus("ACTIVE");
            dbTrack.setPhoneNumber(user.getPhoneNumber());
            dbTrack.setCreatedAt(LocalDateTime.now());
            adminDatabaseRepo.save(dbTrack);

            // 3️⃣ Switch to the new schema
            stmt.execute("USE " + schemaName + ";");
            System.out.println("✅ Switched to schema: " + schemaName);

            // 4️⃣ Initialize tables
            ClassPathResource resource = new ClassPathResource("sql/init.sql");
            String sqlScript = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            System.out.println("📄 SQL Script loaded, length: " + sqlScript.length());

            // Split by semicolon but be careful with triggers/functions
            String[] statements = sqlScript.split(";(?=(?:[^']*'[^']*')*[^']*$)");

            for (int i = 0; i < statements.length; i++) {
                String sql = statements[i].trim();
                if (sql.isEmpty()) continue;

                // Log each statement (for debugging)
                System.out.println("Executing statement " + (i+1) + ": " +
                        sql.substring(0, Math.min(sql.length(), 100)) + "...");

                try {
                    // Ensure CREATE TABLE statements have IF NOT EXISTS
                    if (sql.toUpperCase().startsWith("CREATE TABLE")) {
                        if (!sql.toUpperCase().contains("IF NOT EXISTS")) {
                            sql = sql.replaceFirst("CREATE TABLE", "CREATE TABLE IF NOT EXISTS");
                        }
                    }

                    stmt.execute(sql);
                    System.out.println("✅ Statement " + (i+1) + " executed successfully");

                } catch (SQLException e) {
                    System.err.println("❌ Error executing statement " + (i+1) + ": " + e.getMessage());
                    System.err.println("Failed SQL: " + sql);
                    // Don't throw, continue with other statements
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

    @Transactional(transactionManager = "centralTransactionManager")
    public Status login(LoginDto loginDto) {

        // 1️⃣ Central authentication (OWNER OR EMPLOYEE)
        Authentication auth = authenticationRepo
                .findByPhoneNumberAndActive(loginDto.getPhoneNumber(),true)
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
                employeeLoginRepo.findByUsernameAndPhoneNumber(auth.getUsername(), auth.getPhoneNumber());

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
        response.setRole(auth.getRole());

        //emailService.sendLoginNotification(auth.getEmail(), auth.getUsername());

        return new Status(StatusMessage.SUCCESS, response);
    }


    @Transactional(transactionManager = "centralTransactionManager")

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

    @Transactional(transactionManager = "centralTransactionManager")

    public Status logout(String token) {
        Optional<Session> sessionOpt = sessionRepo.findByToken(token);

        if (sessionOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Invalid token");
        }
        sessionRepo.delete(sessionOpt.get());

        return new Status(StatusMessage.SUCCESS, "Logged out successfully");
    }

    @Transactional(transactionManager = "centralTransactionManager")

    public Status getUserData(String mobileNumber){
        Optional<Authentication> authentication= authenticationRepo.findByPhoneNumberAndActive(mobileNumber,true);

        if(authentication.isPresent()){
            Authentication authentication1= authentication.get();

            if (authentication1.getRole().equals("EMPLOYEE")){
                Optional<Authentication>authentication2= authenticationRepo.findByDatabaseNameAndRoleAndActive(authentication1.getDatabaseName(),"OWNER",true);
                return new Status(StatusMessage.SUCCESS, authentication2);
            }
        }

            return new Status(StatusMessage.SUCCESS,authentication);

    }
}
