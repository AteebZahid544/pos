package com.example.pos.Service;

import com.example.pos.DTO.AssignAuthoritiesDto;
import com.example.pos.DTO.CreateEmployeeDto;
import com.example.pos.config.TenantContext;
import com.example.pos.entity.central.Authentication;
import com.example.pos.entity.central.Authority;
import com.example.pos.entity.central.EmployeeAuthority;
import com.example.pos.entity.central.EmployeeLogin;
import com.example.pos.repo.central.AuthenticationRepo;
import com.example.pos.repo.central.AuthorityRepository;
import com.example.pos.repo.central.EmployeeAuthorityRepository;
import com.example.pos.repo.central.EmployeeLoginRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class EmployeeLoginService {

    @Autowired
    private  EmployeeLoginRepo employeeRepo;

    @Autowired
    private  AuthorityRepository authorityRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationRepo authenticationRepo;

    @Autowired
    private EmployeeAuthorityRepository employeeAuthority;

    @Autowired
    private EmployeeAuthorityRepository employeeAuthorityRepository;

    @Autowired
    private AuthorityRepository authorityRepository;


    @Transactional
    public Status createEmployee(CreateEmployeeDto dto) {

        // 1️⃣ Current tenant (owner ka database)
        String tenantDb = TenantContext.getTenantId();

        // 2️⃣ Fetch OWNER from authentication (same tenant)
        Authentication ownerAuth = authenticationRepo
                .findByDatabaseNameAndRoleAndActive(tenantDb, "OWNER",true)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        // 3️⃣ Check duplicate username / phone
        if (authenticationRepo.existsByUsernameAndPhoneNumberAndActiveTrue(dto.getUsername(), dto.getPhoneNumber())) {
            return new Status(StatusMessage.FAILURE, "Employee already exists");
        }

        // 4️⃣ SAVE IN AUTHENTICATION TABLE (LOGIN PURPOSE)
        Authentication empAuth = new Authentication();
        empAuth.setUsername(dto.getUsername());
        empAuth.setPhoneNumber(dto.getPhoneNumber()); // 👈 IMPORTANT
        empAuth.setPassword(passwordEncoder.encode(dto.getPassword()));
        empAuth.setDatabaseName(tenantDb);
        empAuth.setRole("EMPLOYEE");
        empAuth.setActive(true);

        authenticationRepo.save(empAuth);

        EmployeeLogin employeeLogin= new EmployeeLogin();
        employeeLogin.setUsername(dto.getUsername());               // SAME USERNAME
        employeeLogin.setOwnerName(ownerAuth.getUsername());        // AUTO OWNER
        employeeLogin.setTenantSchema(tenantDb);
        employeeLogin.setPhoneNumber(dto.getPhoneNumber());
        employeeLogin.setActive(true);

        employeeRepo.save(employeeLogin);

        return new Status(StatusMessage.SUCCESS, "Employee created successfully");
    }

    public Status getEmployees(){

        String tenantDb = TenantContext.getTenantId();

        List<EmployeeLogin> employeeLogin= employeeRepo.findAllByTenantSchemaAndIsActive(tenantDb,true);
        if (employeeLogin.isEmpty()){
            return new Status(StatusMessage.FAILURE,"Employees not found");
        }
        return new Status(StatusMessage.SUCCESS, employeeLogin);
    }

    public Status getAuthorities(){
        List<Authority> authorities= authorityRepo.findAll();
        if (authorities.isEmpty()){
            return new Status(StatusMessage.FAILURE,"Authorities not found");
        }
        return new Status(StatusMessage.SUCCESS, authorities);
    }

    @Transactional
    public Status assignAuthorities(AssignAuthoritiesDto dto) {

        EmployeeLogin employee = employeeRepo.findByEmployeeId(dto.getEmployeeId())
                .orElse(null);

        if (employee == null) {
            return new Status(StatusMessage.FAILURE, "Employee not found");
        }

//        // 1️⃣ Delete old authorities first (optional: depends on your logic)
//        employeeAuthority.deleteByEmployeeId(employee.getEmployeeId().toString());

        // 2️⃣ Assign new authorities
        for (Integer authId : dto.getAuthorityIds()) {
            Authority authority = authorityRepo.findById(authId).orElse(null);
            if (authority != null) {
                EmployeeAuthority ea = new EmployeeAuthority();
                ea.setEmployeeId(String.valueOf(employee.getEmployeeId()));
                ea.setAuthorityId(authId);
                ea.setActive(true);
                employeeAuthority.save(ea);

            }
        }

        return new Status(StatusMessage.SUCCESS, "Authorities assigned successfully");
    }

    public Status getEmployeesAndAuthorities() {
        String tenantDb = TenantContext.getTenantId();

        try {
            // Fetch all employees for the tenant
            List<EmployeeLogin> employeeLogin = employeeRepo.findAllByTenantSchemaAndIsActive(tenantDb, true);

            if (employeeLogin.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "Employees not found");
            }

            // Create a list to store the result
            List<Map<String, Object>> employeeAuthoritiesList = new ArrayList<>();

            for (EmployeeLogin employee : employeeLogin) {
                String username = employee.getUsername();
                String employeeId = String.valueOf(employee.getEmployeeId());
                String phoneNumber = employee.getPhoneNumber(); // Get phone number

                // Fetch authority IDs from employee_authority table using employeeId
                List<EmployeeAuthority> employeeAuthorities = employeeAuthorityRepository
                        .findByEmployeeIdAndIsActive(employeeId, true);

                // Create list to store authorities with both ID and name
                List<Map<String, Object>> authoritiesList = new ArrayList<>();

                if (!employeeAuthorities.isEmpty()) {
                    // Extract authority IDs
                    List<Integer> authorityIds = employeeAuthorities.stream()
                            .map(EmployeeAuthority::getAuthorityId)
                            .collect(Collectors.toList());

                    // Fetch authority details from authority table
                    List<Authority> authorities = authorityRepo.findByAuthorityIdIn(authorityIds);

                    // Create map of authorityId -> authority for quick lookup
                    Map<Integer, Authority> authorityMap = authorities.stream()
                            .collect(Collectors.toMap(Authority::getAuthorityId, auth -> auth));

                    // Build authorities list with both ID and name
                    for (EmployeeAuthority empAuth : employeeAuthorities) {
                        Integer authId = empAuth.getAuthorityId();
                        Authority authority = authorityMap.get(authId);

                        Map<String, Object> authData = new HashMap<>();
                        authData.put("id", authId);
                        authData.put("name", authority != null ? authority.getAuthorityName() : "Unknown");

                        authoritiesList.add(authData);
                    }
                }

                // Create response object for each employee
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("username", username);
                employeeData.put("employeeId", employeeId);
                employeeData.put("phoneNumber", phoneNumber != null ? phoneNumber : "");
                employeeData.put("authorities", authoritiesList); // Contains list of objects with id and name

                employeeAuthoritiesList.add(employeeData);
            }
            System.out.println("Employee authorities response: " + employeeAuthoritiesList);

            return new Status(StatusMessage.SUCCESS, employeeAuthoritiesList);

        } catch (Exception e) {
            log.error("Error fetching employees and authorities for tenant: " + tenantDb, e);
            return new Status(StatusMessage.FAILURE, "Error processing request: " + e.getMessage());
        }
    }
    public Status updateEmployeeAuthorities(String employeeId, List<Integer> newAuthorityIds) {
        String tenantDb = TenantContext.getTenantId();

        try {
            // First verify employee exists in current tenant
            Optional<EmployeeLogin> employeeOpt = employeeRepo.findByEmployeeIdAndTenantSchema(
                    Long.valueOf(employeeId), tenantDb);

            if (employeeOpt.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "Employee not found in current tenant");
            }

            // Validate all authority IDs exist (no tenant filter needed)
            if (!newAuthorityIds.isEmpty()) {
                List<Authority> existingAuthorities = authorityRepo
                        .findByAuthorityIdIn(newAuthorityIds);

                if (existingAuthorities.size() != newAuthorityIds.size()) {
                    List<Integer> existingIds = existingAuthorities.stream()
                            .map(Authority::getAuthorityId)
                            .collect(Collectors.toList());

                    List<Integer> missingIds = newAuthorityIds.stream()
                            .filter(id -> !existingIds.contains(id))
                            .collect(Collectors.toList());

                    return new Status(StatusMessage.FAILURE,
                            "Invalid authority IDs: " + missingIds);
                }
            }

            // Get current employee authorities (no tenant filter)
            List<EmployeeAuthority> currentAuthorities = employeeAuthorityRepository
                    .findByEmployeeIdAndIsActive(employeeId,true);

            // Convert to Set for easier comparison
            Set<Integer> currentIds = currentAuthorities.stream()
                    .map(EmployeeAuthority::getAuthorityId)
                    .collect(Collectors.toSet());

            Set<Integer> newIds = new HashSet<>(newAuthorityIds);

            // Find authorities to add
            List<Integer> toAdd = newAuthorityIds.stream()
                    .filter(id -> !currentIds.contains(id))
                    .collect(Collectors.toList());

            // Find authorities to remove
            List<EmployeeAuthority> toRemove = currentAuthorities.stream()
                    .filter(ea -> !newIds.contains(ea.getAuthorityId()))
                    .collect(Collectors.toList());

            // Remove old authorities
            if (!toRemove.isEmpty()) {
                employeeAuthorityRepository.deleteAll(toRemove);
            }

            // Add new authorities
            if (!toAdd.isEmpty()) {
                List<EmployeeAuthority> newAuthorities = toAdd.stream()
                        .map(authorityId -> {
                            EmployeeAuthority ea = new EmployeeAuthority();
                            ea.setEmployeeId(employeeId);
                            ea.setAuthorityId(authorityId);
                            ea.setActive(true);
                            return ea;
                        })
                        .collect(Collectors.toList());

                employeeAuthorityRepository.saveAll(newAuthorities);
            }

            // Get updated authorities for response
            List<EmployeeAuthority> updatedAuthorities = employeeAuthorityRepository
                    .findByEmployeeIdAndIsActive(employeeId,true);

            List<String> authorityNames = updatedAuthorities.stream()
                    .map(EmployeeAuthority::getAuthorityId)
                    .map(authId -> authorityRepo.findByAuthorityId(authId)
                            .map(Authority::getAuthorityName)
                            .orElse("Unknown"))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("employeeId", employeeId);
            response.put("username", employeeOpt.get().getUsername());
            response.put("authorities", authorityNames);
            response.put("added", toAdd.size());
            response.put("removed", toRemove.size());
            response.put("tenant", tenantDb);

            return new Status(StatusMessage.SUCCESS, response);

        } catch (NumberFormatException e) {
            return new Status(StatusMessage.FAILURE, "Invalid employee ID format");
        } catch (Exception e) {
            log.error("Error updating authorities for employee {} in tenant {}: {}",
                    employeeId, tenantDb, e.getMessage(), e);
            return new Status(StatusMessage.FAILURE, "Error updating authorities: " + e.getMessage());
        }
    }

    public Status deleteSpecificEmployeeAuthorities(String employeeId, List<Integer> authorityIdsToRemove) {
        String tenantDb = TenantContext.getTenantId();

        try {
            // First verify employee exists in current tenant
            Optional<EmployeeLogin> employeeOpt = employeeRepo.findByEmployeeIdAndTenantSchema(
                    Long.valueOf(employeeId), tenantDb);

            if (employeeOpt.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "Employee not found in current tenant");
            }

            if (authorityIdsToRemove == null || authorityIdsToRemove.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "No authority IDs provided");
            }

            // Get current employee authorities (no tenant filter)
            List<EmployeeAuthority> currentAuthorities = employeeAuthorityRepository
                    .findByEmployeeIdAndIsActive(employeeId, true);

            if (currentAuthorities.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "No authorities found for employee");
            }

            // Filter authorities to remove
            List<EmployeeAuthority> authoritiesToDelete = currentAuthorities.stream()
                    .filter(ea -> authorityIdsToRemove.contains(ea.getAuthorityId()))
                    .collect(Collectors.toList());

            if (authoritiesToDelete.isEmpty()) {
                return new Status(StatusMessage.FAILURE,
                        "None of the specified authorities exist for this employee");
            }

            // Delete specific authorities (no tenant filter)
            employeeAuthorityRepository.deleteAll(authoritiesToDelete);

            // Get remaining authorities for response
            List<EmployeeAuthority> remainingAuthorities = employeeAuthorityRepository
                    .findByEmployeeIdAndIsActive(employeeId, true);

            List<String> remainingAuthorityNames = remainingAuthorities.stream()
                    .map(EmployeeAuthority::getAuthorityId)
                    .map(authId -> authorityRepo.findByAuthorityId(authId)
                            .map(Authority::getAuthorityName)
                            .orElse("Unknown"))
                    .collect(Collectors.toList());

            List<String> removedAuthorityNames = authoritiesToDelete.stream()
                    .map(EmployeeAuthority::getAuthorityId)
                    .map(authId -> authorityRepo.findByAuthorityId(authId)
                            .map(Authority::getAuthorityName)
                            .orElse("Unknown"))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("employeeId", employeeId);
            response.put("username", employeeOpt.get().getUsername());
            response.put("tenant", tenantDb);
            response.put("removedAuthorities", removedAuthorityNames);
            response.put("remainingAuthorities", remainingAuthorityNames);
            response.put("removedCount", authoritiesToDelete.size());

            return new Status(StatusMessage.SUCCESS, "Authorities removed successfully");

        } catch (NumberFormatException e) {
            return new Status(StatusMessage.FAILURE, "Invalid employee ID format");
        } catch (Exception e) {
            log.error("Error deleting specific authorities for employee {} in tenant {}: {}",
                    employeeId, tenantDb, e.getMessage(), e);
            return new Status(StatusMessage.FAILURE, "Error deleting authorities: " + e.getMessage());
        }
    }

    public Status deleteEmployeeUser(String phoneNumber){

        String tenantDb = TenantContext.getTenantId();

        Authentication authentication= authenticationRepo.findByDatabaseNameAndPhoneNumberAndActive(tenantDb,phoneNumber,true);
        if (authentication == null)
        {
            return new Status(StatusMessage.FAILURE, "Employee not exist");
        }
        authentication.setActive(false);
        authenticationRepo.save(authentication);

        EmployeeLogin employeeLogin= employeeRepo.findByTenantSchemaAndPhoneNumberAndIsActive(tenantDb,phoneNumber,true);

        employeeLogin.setActive(false);
        employeeRepo.save(employeeLogin);

        return new Status(StatusMessage.SUCCESS, "Employee Login deleted successfully");
    }
}
