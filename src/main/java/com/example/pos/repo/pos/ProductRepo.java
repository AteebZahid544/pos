package com.example.pos.repo.pos;

import com.example.pos.entity.pos.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<ProductEntity,String> {
    ProductEntity findByInvoiceNumberAndProductNameAndIsActive(int id,String productName,Boolean isActive);
    @Query("SELECT COALESCE(MAX(p.invoiceNumber), 0) " +
            "FROM ProductEntity p " +
            "WHERE p.status = :status")
    Integer findMaxInvoiceNumberByStatus(@Param("status") String status);

    List<ProductEntity> findAllByInvoiceNumberAndStatusAndIsActive(int invoiceNumber,String status, boolean isActive);

    boolean existsByCategory(String category);

    List<ProductEntity> findByCategory(String category);
    List<ProductEntity> findByProductName(String productName); // productName is stored as ID string
    List<ProductEntity> findByCategoryAndProductName(String category, String productName);
    List<ProductEntity>findByIsActiveTrueAndStatus(@Param("status") String Status);


}
