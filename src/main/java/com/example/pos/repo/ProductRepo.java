package com.example.pos.repo;

import com.example.pos.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepo extends JpaRepository<ProductEntity,String> {
    ProductEntity findByCategory(String category);
    ProductEntity findById(int id);
    ProductEntity findByQuantity(Integer quantity);
    boolean existsByCategory(String category);
}
