package com.example.pos.repo.pos;

import com.example.pos.entity.pos.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesRepo extends JpaRepository<SalesEntity,Integer> {
    Optional<SalesEntity>findById(int id);
    SalesEntity deleteById(int id);
    @Query("SELECT COALESCE(MAX(s.invoiceNumber), 0) " +
            "FROM SalesEntity s " +
            "WHERE s.status = :status")
    Integer findMaxInvoiceNumberByStatus(@Param("status") String status);

    List<SalesEntity> findByIsActiveTrueAndStatus(String status);
    List<SalesEntity>findByCategoryAndProductName(String category, String productName);
    List<SalesEntity>findByProductName(String productName);
    List<SalesEntity>findByCategory(String category);
    List<SalesEntity>findAllByInvoiceNumberAndStatusAndIsActive(int invoiceNumber,String status, Boolean isActive);

}
