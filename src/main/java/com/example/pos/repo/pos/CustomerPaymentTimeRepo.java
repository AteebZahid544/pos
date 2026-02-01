package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyPaymentTime;
import com.example.pos.entity.pos.CustomerPaymentTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPaymentTimeRepo extends JpaRepository<CustomerPaymentTime, Integer> {

    Optional<CustomerPaymentTime> findByInvoiceNumberAndIsActive(int invoiceNumber, Boolean isActive);
    CustomerPaymentTime findByCustomerNameAndPaymentTime(String customerName, LocalDateTime paymentTime);
    @Query("""
    SELECT COALESCE(SUM(c.amountPaid), 0)
    FROM CustomerPaymentTime c
    WHERE c.isActive = true
      AND c.billingMonth = :month
""")
    BigDecimal getTotalAmountPaidByMonth(@Param("month") YearMonth month);

    // Repository method
    @Query("SELECT c FROM CustomerPaymentTime c WHERE " +
            "c.isActive = true AND " +
            "c.amountPaid > 0 AND " +
            "(:startDateTime IS NULL OR c.paymentTime >= :startDateTime) AND " +
            "(:endDateTime IS NULL OR c.paymentTime <= :endDateTime)")
    List<CustomerPaymentTime> findActivePaymentsWithDateRange(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT cpt FROM CustomerPaymentTime cpt " +
            "WHERE cpt.isActive = true " +
            "AND DATE(cpt.paymentTime) = :date " +
            "AND cpt.amountPaid > 0 " +
            "ORDER BY cpt.paymentTime DESC")
    List<CustomerPaymentTime> findByPaymentDate(@Param("date") LocalDate date);
}
