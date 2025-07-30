package com.example.pos.repo;

import com.example.pos.entity.CompanyBillAmountPaid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;

public interface CompanyBillAmountPaidRepo extends JpaRepository<CompanyBillAmountPaid,String> {
    CompanyBillAmountPaid findByVendorName(String vendorName);
   CompanyBillAmountPaid findByVendorNameAndBillingMonth(String vendorName, YearMonth billingMonth);

    CompanyBillAmountPaid findTopByVendorNameOrderByBillingMonthDesc(String vendorName);

}
