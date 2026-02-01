package com.example.pos.repo.pos;

import com.example.pos.DTO.SalesDto;
import com.example.pos.entity.pos.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<ProductEntity,String> {
    ProductEntity findByInvoiceNumberAndProductNameAndIsActive(int id,String productName,Boolean isActive);
    @Query("SELECT COALESCE(MAX(p.invoiceNumber), 0) " +
            "FROM ProductEntity p " +
            "WHERE p.status = :status")
    Integer findMaxInvoiceNumberByStatus(@Param("status") String status);

    List<ProductEntity> findAllByInvoiceNumberAndStatusAndIsActive(int invoiceNumber,String status, boolean isActive);

    @Query("SELECT p FROM ProductEntity p WHERE " +
            "p.isActive = true AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:productName IS NULL OR p.productName = :productName) AND " +
            "((:startDateTime IS NULL OR " +
            "  (:status = 'Purchase' AND p.productEntryTime >= :startDateTime) OR " +
            "  (:status = 'Return' AND p.returnTime >= :startDateTime) OR " +
            "  p.recordUpdatedTime >= :startDateTime) AND " +
            "(:endDateTime IS NULL OR " +
            "  (:status = 'Purchase' AND p.productEntryTime <= :endDateTime) OR " +
            "  (:status = 'Return' AND p.returnTime <= :endDateTime) OR " +
            "  p.recordUpdatedTime <= :endDateTime))")
    List<ProductEntity> findWithStatusBasedFilters(
            @Param("status") String status,
            @Param("category") String category,
            @Param("productName") String productName,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT " +
            "COALESCE(SUM(p.totalPrice), 0) - " +
            "COALESCE((SELECT SUM(r.totalPrice) FROM ProductEntity r " +
            "          WHERE FUNCTION('YEAR', r.returnTime) = :year " +
            "          AND FUNCTION('MONTH', r.returnTime) = :month " +
            "          AND r.isActive = true), 0) " +
            "FROM ProductEntity p " +
            "WHERE FUNCTION('YEAR', p.productEntryTime) = :year " +
            "AND FUNCTION('MONTH', p.productEntryTime) = :month " +
            "AND p.isActive = true")
    BigDecimal getGrandTotalByMonth(
            @Param("year") int year,
            @Param("month") int month
    );

    List<ProductEntity> findByInvoiceNumberAndIsActive(Integer invoiceNumber, Boolean isActive);

    // Find latest product entry time for an invoice
    @Query("SELECT MAX(p.productEntryTime) FROM ProductEntity p WHERE p.invoiceNumber = :invoiceNumber")
    LocalDateTime findLatestProductTimeByInvoice(@Param("invoiceNumber") Integer invoiceNumber);

}
