package com.example.pos.repo.pos;

import com.example.pos.entity.pos.Customers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customers, Long> {

    Customers findByCustomerNameAndIsActiveTrue(String name);
    List<Customers>findByIsActive(Boolean isActive);
    Optional<Customers> findById(Long id);
}
