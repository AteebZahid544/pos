package com.example.pos.repo;

import com.example.pos.entity.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthenticationRepo extends JpaRepository<Authentication,String> {
    Authentication findByPassword(String password);
    Authentication findByUsername(String username);
}
