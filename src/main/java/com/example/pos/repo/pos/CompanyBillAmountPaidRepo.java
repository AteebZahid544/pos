package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyBillAmountPaid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;

@Repository
public interface CompanyBillAmountPaidRepo extends JpaRepository<CompanyBillAmountPaid,String> {
    CompanyBillAmountPaid findByVendorName(String vendorName);
   CompanyBillAmountPaid findByVendorNameAndBillingMonth(String vendorName, YearMonth billingMonth);

    CompanyBillAmountPaid findTopByVendorNameOrderByBillingMonthDesc(String vendorName);

}
