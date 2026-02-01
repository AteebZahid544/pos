package com.example.pos.repo.central;

import com.example.pos.entity.central.EmployeeLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeLoginRepo extends JpaRepository<EmployeeLogin,String> {

    Optional<EmployeeLogin> findByUsername(String username);
    Optional<EmployeeLogin> findByEmployeeId(Long id);

}
