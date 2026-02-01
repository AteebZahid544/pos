package com.example.pos.repo.pos;

import com.example.pos.DTO.CustomerPurchaseDto;
import com.example.pos.DTO.CustomerRequestDTO;
import com.example.pos.entity.pos.CustomerInvoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CustomerInvoiceRecordRepo extends JpaRepository<CustomerInvoiceRecord,String> {
    Optional<CustomerInvoiceRecord> findByInvoiceNumberAndStatusAndIsActive(Integer invoiceNumber,String status, Boolean isActive);
    List<CustomerInvoiceRecord>
    findByCustomerNameAndIsActiveTrueAndStatusInOrderByIdDesc(String customerName,List<String> statuses);

    @Query(value = """
    SELECT 
        c.customer_name AS customerName,
        COALESCE(SUM(
            CASE 
                WHEN c.status = 'Sale' THEN 
                    COALESCE(c.rent, 0) - 
                    COALESCE(c.discount, 0) + 
                    COALESCE(c.grand_total, 0) + 
                    COALESCE(c.amount_paid, 0)
                WHEN c.status = 'Return' THEN 
                    -(COALESCE(c.rent, 0) - 
                      COALESCE(c.discount, 0) + 
                      COALESCE(c.grand_total, 0))
                ELSE 0
            END
        ), 0) AS totalPurchase
    FROM customer_invoice_record c
    WHERE c.is_active = 1
      AND c.billing_month = :billingMonth
      AND c.status IN ('Sale', 'Return')
    GROUP BY c.customer_name
    ORDER BY COALESCE(SUM(
        CASE 
            WHEN c.status = 'Sale' THEN 
                COALESCE(c.rent, 0) - 
                COALESCE(c.discount, 0) + 
                COALESCE(c.grand_total, 0) + 
                COALESCE(c.amount_paid, 0)
            WHEN c.status = 'Return' THEN 
                -(COALESCE(c.rent, 0) - 
                  COALESCE(c.discount, 0) + 
                  COALESCE(c.grand_total, 0))
            ELSE 0
        END
    ), 0) DESC
    LIMIT 10
""", nativeQuery = true)
    List<Object[]> getTopCustomersRaw(@Param("billingMonth") String billingMonth);

    // Custom query for ledger with YearMonth range
    @Query("SELECT cir FROM CustomerInvoiceRecord cir " +
            "WHERE cir.customerName = :customerName " +
            "AND cir.isActive = true " +
            "AND cir.status IN :statuses " +
            "AND cir.billingMonth BETWEEN :startMonth AND :endMonth " +
            "ORDER BY cir.billingMonth, cir.id ASC")
    List<CustomerInvoiceRecord> findCustomerLedgerByMonthRange(
            @Param("customerName") String customerName,
            @Param("statuses") List<String> statuses,
            @Param("startMonth") YearMonth startMonth,
            @Param("endMonth") YearMonth endMonth);

    // Alternative: Query for specific month
    @Query("SELECT cir FROM CustomerInvoiceRecord cir " +
            "WHERE cir.customerName = :customerName " +
            "AND cir.isActive = true " +
            "AND cir.status IN :statuses " +
            "AND cir.billingMonth = :billingMonth " +
            "ORDER BY cir.id ASC")
    List<CustomerInvoiceRecord> findCustomerLedgerByMonth(
            @Param("customerName") String customerName,
            @Param("statuses") List<String> statuses,
            @Param("billingMonth") YearMonth billingMonth);

    // Find customer invoices by date range
    @Query("SELECT cir FROM CustomerInvoiceRecord cir " +
            "WHERE cir.isActive = true " +
            "AND DATE(cir.saleDate) = :date " + // Assuming you have saleDate field
            "ORDER BY cir.id DESC")
    List<CustomerInvoiceRecord> findCustomerInvoicesByDate(@Param("date") LocalDate date);

    @Query("SELECT " +
            "COALESCE(SUM(CASE " +
            "    WHEN p.status = 'Sale' THEN " +
            "        COALESCE(p.rent, 0) - " +
            "        COALESCE(p.discount, 0) + " +
            "        COALESCE(p.grandTotal, 0) + " +  // Added + operator
            "        COALESCE(p.amountPaid, 0) " +    // Changed amount_paid to amountPaid and added + operator
            "    WHEN p.status = 'Return' THEN " +
            "        -(COALESCE(p.rent, 0) - " +      // Added - sign for Return and opening parenthesis
            "          COALESCE(p.discount, 0) + " +  // Changed ) to + operator
            "          COALESCE(p.grandTotal, 0)) " + // Added closing parenthesis
            "    ELSE 0 " +
            "END), 0) " +
            "FROM CustomerInvoiceRecord p " +
            "WHERE YEAR(p.saleDate) = :year " +
            "AND MONTH(p.saleDate) = :month " +
            "AND p.status IN ('Sale', 'Return') " +
            "AND p.isActive = true")
    BigDecimal getNetSaleAmountByMonth(
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("SELECT " +
            "COALESCE(SUM(" +
            "    CASE " +
            "        WHEN c.status = 'Sale' THEN c.totalPurchaseCost " +
            "        WHEN c.status = 'Return' THEN -c.totalPurchaseCost " +
            "        ELSE 0 " +
            "    END" +
            "), 0) " +
            "FROM CustomerInvoiceRecord c " +
            "WHERE YEAR(c.saleDate) = :year " +
            "AND MONTH(c.saleDate) = :month " +
            "AND c.isActive = true")
    BigDecimal getNetPurchaseCostByMonth(
            @Param("year") int year,
            @Param("month") int month
    );
}

