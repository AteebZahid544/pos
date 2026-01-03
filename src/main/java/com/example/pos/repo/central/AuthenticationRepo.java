package com.example.pos.repo.central;

import com.example.pos.entity.central.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthenticationRepo extends JpaRepository<Authentication,String> {

    Optional<Authentication> findByEmail(String email);
    Optional<Authentication> findByPhoneNumber(String phoneNumber);
}
