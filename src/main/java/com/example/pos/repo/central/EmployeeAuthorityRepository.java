package com.example.pos.repo.central;

import com.example.pos.entity.central.EmployeeAuthority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeAuthorityRepository extends JpaRepository<EmployeeAuthority, Long> {

    @Query("SELECT a.authorityName FROM EmployeeAuthority ea JOIN Authority a ON ea.authorityId = a.authorityId WHERE ea.employeeId = :employeeId")
    List<String> findAuthoritiesByEmployeeId(@Param("employeeId") String employeeId);

    void deleteByEmployeeId(String employeeId);
}