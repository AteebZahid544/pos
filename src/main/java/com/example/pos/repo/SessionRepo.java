package com.example.pos.repo;

import com.example.pos.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepo extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
    void deleteByPhoneNumber(String phoneNumber); // Optional: to clean old sessions
}
