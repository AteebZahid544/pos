package com.example.pos.repo.pos;

import com.example.pos.entity.pos.Vendor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Vendor findByVendorName(String name);
    List<Vendor>findByIsActive(Boolean isActive);
    Optional<Vendor> findById(Long id);
}
