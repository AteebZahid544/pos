package com.example.pos.repo;

import com.example.pos.entity.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthenticationRepo extends JpaRepository<Authentication,String> {
    Optional<Authentication> findByUsernameAndPassword(String username, String password);
    Optional<Authentication> findByUsername(String username);
    Optional<Authentication> findByEmail(String email);
    Optional<Authentication> findByPhoneNumber(String phoneNumber);
}
