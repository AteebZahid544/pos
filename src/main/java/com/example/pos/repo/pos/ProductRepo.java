package com.example.pos.repo.pos;

import com.example.pos.entity.pos.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<ProductEntity,String> {
    ProductEntity findById(int id);
    ProductEntity findByIdAndIsActive(int id, Boolean isActive);
    ProductEntity findByQuantity(Integer quantity);
    boolean existsByCategory(String category);

    List<ProductEntity> findByCategory(String category);
    List<ProductEntity> findByProductName(String productName); // productName is stored as ID string
    List<ProductEntity> findByCategoryAndProductName(String category, String productName);

}
