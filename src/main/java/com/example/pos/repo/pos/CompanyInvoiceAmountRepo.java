package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyInvoiceAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyInvoiceAmountRepo extends JpaRepository<CompanyInvoiceAmount,String> {
    Optional<CompanyInvoiceAmount> findByInvoiceNumberAndStatusAndIsActive(Integer invoiceNumber,@Param("status") String status, Boolean isActive);

}
