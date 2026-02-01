package com.example.pos.repo.pos;

import com.example.pos.entity.pos.ProductStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductStepRepository extends JpaRepository<ProductStep, Long> {
    List<ProductStep> findByProductIdOrderByStepOrderAsc(Long productId);
}