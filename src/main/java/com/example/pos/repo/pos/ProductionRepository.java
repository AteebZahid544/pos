package com.example.pos.repo.pos;

import com.example.pos.entity.pos.ProductManufacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductionRepository extends JpaRepository<ProductManufacture, Long> {
    @Query("SELECT DISTINCT p FROM ProductManufacture p " +
            "LEFT JOIN FETCH p.steps s " +
            "WHERE p.id = :productId " +
            "ORDER BY s.stepOrder")
    Optional<ProductManufacture> findByIdWithSteps(@Param("productId") Long productId);

    List<ProductManufacture> findAll();

}