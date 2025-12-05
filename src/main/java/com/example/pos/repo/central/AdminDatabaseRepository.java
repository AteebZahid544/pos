package com.example.pos.repo.central;

import com.example.pos.entity.central.AdminDatabase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminDatabaseRepository extends JpaRepository<AdminDatabase, Long> {
    Optional<AdminDatabase> findByUsername(String username);
}

