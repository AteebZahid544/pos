package com.example.pos.repo.pos;

import com.example.pos.entity.pos.ProductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {

    // Find active productions with steps eagerly fetched
    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps WHERE pr.endTime IS NULL ORDER BY pr.startTime DESC")
    List<ProductionRecord> findByEndTimeIsNullWithSteps();

    // Find user's active productions with steps
    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps WHERE pr.userId = :userId AND pr.endTime IS NULL ORDER BY pr.startTime DESC")
    List<ProductionRecord> findByUserIdAndEndTimeIsNullWithSteps(@Param("userId") Long userId);

    // Find production by ID with steps
    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps WHERE pr.id = :id")
    Optional<ProductionRecord> findByIdWithSteps(@Param("id") Long id);


    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps WHERE pr.userId = :userId AND pr.status = :status AND pr.endTime IS NULL ORDER BY pr.startTime DESC")
    List<ProductionRecord> findByUserIdAndStatusAndEndTimeIsNull(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps WHERE pr.userId = :userId AND pr.status = :status ORDER BY pr.startTime DESC")
    List<ProductionRecord> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps WHERE pr.status = :status AND pr.endTime IS NULL ORDER BY pr.startTime DESC")
    List<ProductionRecord> findByStatusAndEndTimeIsNull(@Param("status") String status);

    // Add this method for resumable productions
    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps WHERE pr.userId = :userId AND pr.status IN ('ACTIVE', 'PAUSED') AND pr.endTime IS NULL ORDER BY pr.startTime DESC")
    List<ProductionRecord> findResumableProductions(@Param("userId") Long userId);

    // Find productions by date with steps
    @Query("SELECT DISTINCT pr FROM ProductionRecord pr LEFT JOIN FETCH pr.steps " +
            "WHERE DATE(pr.startTime) = :date OR DATE(pr.endTime) = :date " +
            "ORDER BY pr.startTime DESC")
    List<ProductionRecord> findByDateWithSteps(@Param("date") LocalDate date);
}