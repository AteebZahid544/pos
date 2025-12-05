package com.example.pos.repo.pos;

import com.example.pos.entity.pos.Category;
import com.example.pos.entity.pos.ProductName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductNameRepository extends JpaRepository<ProductName, Long> {
    Optional<ProductName> findByProductName(String productName);
    Optional<ProductName> findProductByIdAndIsActiveTrue(Long id);
    Optional<ProductName> findByCategoryAndProductNameIgnoreCaseAndIsActiveTrue(Category category, String productName);
    Optional<ProductName> findByProductNameIgnoreCase(String productName);
    Optional<ProductName>findByProductNameAndIsActive(String productName, Boolean isActive);

}

