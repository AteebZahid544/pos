package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyPaymentTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CompanyPaymentTimeRepo extends JpaRepository<CompanyPaymentTime, Integer> {

    Optional<CompanyPaymentTime> findByInvoiceNumberAndIsActive(int invoiceNumber,Boolean isActive);
    CompanyPaymentTime findByVendorNameAndPaymentTime(String vendorName, LocalDateTime paymentTime);
}
