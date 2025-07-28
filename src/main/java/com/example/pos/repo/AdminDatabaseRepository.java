package com.example.pos.repo;

import com.example.pos.entity.AdminDatabase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminDatabaseRepository extends JpaRepository<AdminDatabase, Long> {
    Optional<AdminDatabase> findByUsername(String username);
}

