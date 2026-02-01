package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyBillAmountPaid;
import com.example.pos.entity.pos.CustomerBillAmountPaid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.YearMonth;

@Repository
public interface CustomerBillAmountPaidRepo extends JpaRepository<CustomerBillAmountPaid,String> {

    CustomerBillAmountPaid findById(int id);


    CustomerBillAmountPaid findByCustomerNameAndBillingMonth(String customer, YearMonth billingMonth);

    CustomerBillAmountPaid findTopByCustomerNameOrderByBillingMonthDesc(String customer);




    @Query("""
SELECT SUM(c.balance)
FROM CustomerBillAmountPaid c
WHERE c.billingMonth = (
    SELECT MAX(c2.billingMonth)
    FROM CustomerBillAmountPaid c2
    WHERE c2.customerName = c.customerName
      AND c2.billingMonth <= :billingMonth
)
""")
    BigDecimal getCustomerBalanceSnapshot(@Param("billingMonth") YearMonth billingMonth);

}

