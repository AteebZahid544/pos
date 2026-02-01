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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@NoArgsConstructor
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


//    public EmployeeLoginService(EmployeeLoginRepo employeeRepo, AuthorityRepository authorityRepo){
//        this.employeeRepo = employeeRepo;
//        this.authorityRepo = authorityRepo;
//    }

    // Owner adds employee + assigns authorities
    public EmployeeLogin createEmployee(String username, String password, String ownerName, String tenantSchema, List<String> authorityNames){
        EmployeeLogin employee = new EmployeeLogin();
        employee.setUsername(username);
//        employee.setPassword(password); // hash in prod
        employee.setOwnerName(ownerName);
        employee.setTenantSchema(tenantSchema);

        // Fetch Authority objects
        List<Authority> auths = authorityRepo.findAll().stream()
                .filter(a -> authorityNames.contains(a.getAuthorityName()))
                .toList();

        employee.setAuthorities(auths);

        return employeeRepo.save(employee);
    }

    // Login (Owner or Employee)
    public EmployeeLogin login(String username, String password){
        EmployeeLogin emp = employeeRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));



        return emp; // includes authorities
    }

    // Fetch authorities for employee
    public List<String> getAuthorities(String username){
        EmployeeLogin emp = employeeRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return emp.getAuthorities().stream()
                .map(Authority::getAuthorityName)
                .toList();
    }

    @Transactional
    public Status createEmployee(CreateEmployeeDto dto) {

        // 1️⃣ Current tenant (owner ka database)
        String tenantDb = TenantContext.getTenantId();

        // 2️⃣ Fetch OWNER from authentication (same tenant)
        Authentication ownerAuth = authenticationRepo
                .findByDatabaseNameAndRole(tenantDb, "OWNER")
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        // 3️⃣ Check duplicate username / phone
        if (authenticationRepo.existsByUsernameAndPhoneNumber(dto.getUsername(), dto.getPhoneNumber())) {
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

        // 5️⃣ SAVE IN EMPLOYEE_LOGIN TABLE (BUSINESS PURPOSE)
        EmployeeLogin emp = new EmployeeLogin();
        emp.setUsername(dto.getUsername());               // SAME USERNAME
        emp.setOwnerName(ownerAuth.getUsername());        // AUTO OWNER
        emp.setTenantSchema(tenantDb);
        emp.setPhoneNumber(dto.getPhoneNumber());

        employeeRepo.save(emp);

        return new Status(StatusMessage.SUCCESS, "Employee created successfully");
    }

    public Status getEmployees(){
        List<EmployeeLogin> employeeLogin= employeeRepo.findAll();
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
                ea.setEmployeeId(employee.getEmployeeId().toString());
                ea.setAuthorityId(authId);
                employeeAuthority.save(ea);

            }
        }

        return new Status(StatusMessage.SUCCESS, "Authorities assigned successfully");
    }

}
