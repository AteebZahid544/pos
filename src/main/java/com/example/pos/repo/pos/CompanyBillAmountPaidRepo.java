package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyBillAmountPaid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Repository
public interface CompanyBillAmountPaidRepo extends JpaRepository<CompanyBillAmountPaid,String> {
    CompanyBillAmountPaid findByVendorName(String vendorName);
   CompanyBillAmountPaid findByVendorNameAndBillingMonth(String vendorName, YearMonth billingMonth);

    CompanyBillAmountPaid findTopByVendorNameOrderByBillingMonthDesc(String vendorName);

//    @Query("""
//    SELECT COALESCE(SUM(i.balance), 0)
//    FROM CompanyBillAmountPaid i
//    WHERE i.billingMonth = :month
//""")
//    BigDecimal getCompanyBalanceByMonth(@Param("month") YearMonth month);

    @Query("""
SELECT SUM(c.balance)
FROM CompanyBillAmountPaid c
WHERE c.billingMonth = (
    SELECT MAX(c2.billingMonth)
    FROM CompanyBillAmountPaid c2
    WHERE c2.vendorName = c.vendorName
      AND c2.billingMonth <= :billingMonth
)
""")
    BigDecimal getCompanyBalanceSnapshot(@Param("billingMonth") YearMonth billingMonth);


}
