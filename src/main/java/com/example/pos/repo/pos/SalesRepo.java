package com.example.pos.repo.pos;

import com.example.pos.DTO.GraphSalesDto;
import com.example.pos.DTO.SalesDto;
import com.example.pos.entity.pos.ProductEntity;
import com.example.pos.entity.pos.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesRepo extends JpaRepository<SalesEntity,Integer> {
    Optional<SalesEntity>findById(int id);
    SalesEntity deleteById(int id);
    @Query("SELECT COALESCE(MAX(s.invoiceNumber), 0) " +
            "FROM SalesEntity s " +
            "WHERE s.status = :status")
    Integer findMaxInvoiceNumberByStatus(@Param("status") String status);

    List<SalesEntity>findAllByInvoiceNumberAndStatusAndIsActive(int invoiceNumber,String status, Boolean isActive);
    List<SalesEntity> findByInvoiceNumberAndIsActive(Integer invoiceNumber, Boolean isActive);

    @Query("""
    SELECT new com.example.pos.DTO.GraphSalesDto(
        p.productName,
        COALESCE(SUM(
            CASE 
                WHEN (DATE(p.saleEntryTime) BETWEEN :startDate AND :endDate 
                     AND (p.status = 'Sale' OR p.status IS NULL))
                THEN p.totalPrice
                
                WHEN (DATE(p.recordUpdatedTime) BETWEEN :startDate AND :endDate 
                     AND (p.status = 'Sale' OR p.status IS NULL))
                THEN p.totalPrice
                
                WHEN (DATE(p.returnTime) BETWEEN :startDate AND :endDate 
                     AND p.status = 'Return')
                THEN -p.totalPrice
                
                ELSE 0.0
            END
        ), 0.0)
    )
    FROM SalesEntity p
    WHERE (p.isActive IS NULL OR p.isActive = true)
      AND (
        (DATE(p.saleEntryTime) BETWEEN :startDate AND :endDate 
         AND (p.status = 'Sale' OR p.status IS NULL))
        OR
        (p.recordUpdatedTime IS NOT NULL 
         AND DATE(p.recordUpdatedTime) BETWEEN :startDate AND :endDate 
         AND (p.status = 'Sale' OR p.status IS NULL))
        OR
        (p.returnTime IS NOT NULL 
         AND DATE(p.returnTime) BETWEEN :startDate AND :endDate 
         AND p.status = 'Return')
      )
    GROUP BY p.productName
    ORDER BY COALESCE(SUM(
        CASE 
            WHEN (DATE(p.saleEntryTime) BETWEEN :startDate AND :endDate 
                 AND (p.status = 'Sale' OR p.status IS NULL))
            THEN p.totalPrice
            
            WHEN (DATE(p.recordUpdatedTime) BETWEEN :startDate AND :endDate 
                 AND (p.status = 'Sale' OR p.status IS NULL))
            THEN p.totalPrice
            
            WHEN (DATE(p.returnTime) BETWEEN :startDate AND :endDate 
                 AND p.status = 'Return')
            THEN -p.totalPrice
            
            ELSE 0.0
        END
    ), 0.0) DESC
""")
    List<GraphSalesDto> getTopProductsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);


    @Query("SELECT p FROM SalesEntity p WHERE " +
            "p.isActive = true AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:productName IS NULL OR p.productName = :productName) AND " +
            "((:startDateTime IS NULL OR " +
            "  (:status = 'Sale' AND p.saleEntryTime >= :startDateTime) OR " +
            "  (:status = 'Return' AND p.returnTime >= :startDateTime) OR " +
            "  p.recordUpdatedTime >= :startDateTime) AND " +
            "(:endDateTime IS NULL OR " +
            "  (:status = 'Sale' AND p.saleEntryTime <= :endDateTime) OR " +
            "  (:status = 'Return' AND p.returnTime <= :endDateTime) OR " +
            "  p.recordUpdatedTime <= :endDateTime))")
    List<SalesEntity> findWithStatusBasedFilters(
            @Param("status") String status,
            @Param("category") String category,
            @Param("productName") String productName,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    // Find latest sale time for an invoice
    @Query("SELECT MAX(s.saleEntryTime) FROM SalesEntity s WHERE s.invoiceNumber = :invoiceNumber")
    LocalDateTime findLatestSaleTimeByInvoice(@Param("invoiceNumber") Integer invoiceNumber);

}
