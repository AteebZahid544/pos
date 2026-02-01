package com.example.pos.repo.pos;

import com.example.pos.entity.pos.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepo extends JpaRepository<InventoryEntity, Long> {
//    List<InventoryEntity> findAllByProductOrderByStockEntryTimeAsc(ProductEntity product);
InventoryEntity findFirstByCategoryAndProductNameOrderByAddedMonthDesc(
        String category,
        String productName
);

List<InventoryEntity> findFirstByOrderByCategoryDesc();

    @Query(value = "SELECT i1.* FROM inventory i1 " +
            "INNER JOIN ( " +
            "   SELECT category, MAX(added_month) as max_month " +
            "   FROM inventory " +
            "   GROUP BY category " +
            ") i2 ON i1.category = i2.category AND i1.added_month = i2.max_month",
            nativeQuery = true)
    List<InventoryEntity> findLatestRecordForEachCategory();

    @Query("""
SELECT SUM(i.totalPrice)
FROM InventoryEntity i
WHERE i.addedMonth = (
    SELECT MAX(i2.addedMonth)
    FROM InventoryEntity i2
    WHERE i2.productName = i.productName
      AND i2.category = i.category
      AND i2.addedMonth <= :month
)
""")
    BigDecimal getInventorySnapshotValue(@Param("month") YearMonth month);


    List<InventoryEntity> findByAddedMonthOrderByAddedMonthDesc(YearMonth addedMonth);

    List<InventoryEntity> findByCategoryAndAddedMonthOrderByAddedMonthDesc(
            String category, YearMonth addedMonth);



//    Optional<InventoryEntity> findByCategoryAndProductNameAndAddedMonthBetween(
//            String category, String productName, LocalDate startDate, LocalDate endDate);
    InventoryEntity findByCategoryAndProductNameAndAddedMonth(String category, String productName, YearMonth addOn);

}