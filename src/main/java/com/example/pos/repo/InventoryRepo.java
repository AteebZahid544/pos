package com.example.pos.repo;

import com.example.pos.entity.InventoryEntity;
import com.example.pos.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepo extends JpaRepository<InventoryEntity, Long> {
//    List<InventoryEntity> findAllByProductOrderByStockEntryTimeAsc(ProductEntity product);
    InventoryEntity findByCategory(String category);
}