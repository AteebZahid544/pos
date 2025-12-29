package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CustomerBillAmountPaid;
import com.example.pos.entity.pos.CustomersBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;

@Repository
public interface CustomerBillAmountPaidRepo extends JpaRepository<CustomerBillAmountPaid,String> {

    CustomerBillAmountPaid findById(int id);

    CustomerBillAmountPaid findByCustomerName(String customerName);
    CustomerBillAmountPaid findTopByCustomerNameOrderByIdDesc(String customerName);

    @Query(value = "SELECT cb.* FROM customers_balance cb " +
            "INNER JOIN (SELECT customer_name, MAX(id) AS max_id FROM customers_balance GROUP BY customer_name) t " +
            "ON cb.customer_name = t.customer_name AND cb.id = t.max_id",
            nativeQuery = true)
    List<CustomerBillAmountPaid> findLatestBalanceForAllCustomers();

    @Query("SELECT c FROM CustomersBill c WHERE c.customerName = :customerName ORDER BY c.payBillTime DESC")
    List<CustomerBillAmountPaid> findLatestBalanceByCustomerName(@Param("customerName") String customerName);

    CustomerBillAmountPaid findByCustomerNameAndBillingMonth(String customer, YearMonth billingMonth);

    CustomerBillAmountPaid findTopByCustomerNameOrderByBillingMonthDesc(String customer);

}

