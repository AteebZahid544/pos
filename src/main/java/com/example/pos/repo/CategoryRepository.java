package com.example.pos.repo;

import com.example.pos.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCategoryNameAndIsActiveTrue(String categoryName);

    List<Category> findAllByIsActiveTrue();
}

