package com.example.pos.repo.pos;

import com.example.pos.entity.pos.Category;
import com.example.pos.entity.pos.ProductName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductNameRepository extends JpaRepository<ProductName, Long> {
    List<ProductName> findByIsActive(Boolean isActive);

    Optional<ProductName> findProductByIdAndIsActiveTrue(int id);
    Optional<ProductName> findByProductNameAndIsActiveTrue(String productName);
    Optional<ProductName> findByProductNameIgnoreCase(String productName);
    Optional<ProductName>findByProductNameAndIsActive(String productName, Boolean isActive);

}

