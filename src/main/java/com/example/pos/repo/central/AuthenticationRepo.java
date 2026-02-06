package com.example.pos.repo.central;

import com.example.pos.entity.central.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthenticationRepo extends JpaRepository<Authentication,String> {

    Optional<Authentication> findByEmail(String email);
    Optional<Authentication> findByPhoneNumberAndActive(String phoneNumber, boolean isActive);
    Optional<Authentication> findByDatabaseNameAndRoleAndActive(String databaseName,String role, boolean isActive);
    boolean existsByUsernameAndPhoneNumberAndActiveTrue(String userName, String phoneNumber);
    Authentication findByDatabaseNameAndPhoneNumberAndActive(String database, String phoneNumber, boolean isActive);
    Optional<Authentication> findByPhoneNumber(String phoneNumber);

}
