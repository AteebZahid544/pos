package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CustomerInvoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerInvoiceRecordRepo extends JpaRepository<CustomerInvoiceRecord,String> {
    Optional<CustomerInvoiceRecord> findByInvoiceNumberAndStatusAndIsActive(Integer invoiceNumber,String status, Boolean isActive);
    List<CustomerInvoiceRecord>
    findByCustomerNameAndIsActiveTrueAndStatusInOrderByIdAsc(String customerName,List<String> statuses);
}
