package com.example.pos.repo;

import com.example.pos.entity.Category;
import com.example.pos.entity.ProductName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductNameRepository extends JpaRepository<ProductName, Long> {
    Optional<ProductName> findByProductName(String productName);
    Optional<ProductName> findProductByIdAndIsActiveTrue(Long id);
    Optional<ProductName> findByCategoryAndProductNameIgnoreCaseAndIsActiveTrue(Category category, String productName);


}

