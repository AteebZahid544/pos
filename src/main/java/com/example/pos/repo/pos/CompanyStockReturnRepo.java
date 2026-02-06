//package com.example.pos.repo.pos;
//
//import com.example.pos.entity.pos.CompanyStockReturn;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//public interface CompanyStockReturnRepo
//        extends JpaRepository<CompanyStockReturn, Integer> {
//    @Query("SELECT COALESCE(MAX(p.invoiceNumber), 0) " +
//            "FROM ProductEntity p " +
//            "WHERE p.status = :status")
//    Integer findMaxInvoiceNumberByStatus(@Param("status") String status);
//
//}
