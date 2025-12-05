package com.example.pos.repo.pos;

import com.example.pos.entity.pos.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepo extends JpaRepository<InventoryEntity, Long> {
//    List<InventoryEntity> findAllByProductOrderByStockEntryTimeAsc(ProductEntity product);
    InventoryEntity findByCategoryAndProductName(String category, String productName);

}