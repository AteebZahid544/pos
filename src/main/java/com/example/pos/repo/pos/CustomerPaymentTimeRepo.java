package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CustomerPaymentTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerPaymentTimeRepo extends JpaRepository<CustomerPaymentTime, Integer> {

    Optional<CustomerPaymentTime> findByInvoiceNumber(int invoiceNumber);
}
