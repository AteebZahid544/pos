package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CustomerPaymentTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPaymentTimeRepo extends JpaRepository<CustomerPaymentTime, Integer> {

    Optional<CustomerPaymentTime> findByInvoiceNumberAndIsActive(int invoiceNumber, Boolean isActive);
    List<CustomerPaymentTime> findAllByCustomerNameAndIsActiveOrderByPaymentTimeDesc(
            String customerName,
            Boolean isActive
    );
    List<CustomerPaymentTime> findByCustomerNameAndIsActiveTrue(String customerName);

}
