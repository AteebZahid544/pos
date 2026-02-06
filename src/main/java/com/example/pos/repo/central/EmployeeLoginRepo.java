package com.example.pos.repo.central;

import com.example.pos.entity.central.EmployeeLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeLoginRepo extends JpaRepository<EmployeeLogin,String> {

    Optional<EmployeeLogin> findByUsernameAndPhoneNumber(String username, String phoneNumber);
    Optional<EmployeeLogin> findByEmployeeId(Long id);
    List<EmployeeLogin>findAllByTenantSchemaAndIsActive(String tenantSchema, boolean isActive);
    Optional<EmployeeLogin>findByEmployeeIdAndTenantSchema(Long employeeId, String tenantSchema);
    EmployeeLogin findByTenantSchemaAndPhoneNumberAndIsActive(String tenantSchema, String phoneNumber, boolean isActive);


}
