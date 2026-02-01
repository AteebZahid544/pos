package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyInvoiceAmount;
import com.example.pos.entity.pos.CustomerInvoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CompanyInvoiceAmountRepo extends JpaRepository<CompanyInvoiceAmount,String> {
    Optional<CompanyInvoiceAmount> findByInvoiceNumberAndStatusAndIsActive(Integer invoiceNumber,@Param("status") String status, Boolean isActive);
    List<CompanyInvoiceAmount>
    findByVendorNameAndIsActiveTrueAndStatusInOrderByIdDesc(String vendorName,List<String> statuses);

    @Query("SELECT cia FROM CompanyInvoiceAmount cia " +
            "WHERE cia.vendorName = :vendorName " +
            "AND cia.isActive = true " +
            "AND cia.status IN :statuses " +
            "AND cia.billingMonth BETWEEN :startMonth AND :endMonth " +
            "ORDER BY cia.billingMonth, cia.id ASC")
    List<CompanyInvoiceAmount> findVendorLedgerByMonthRange(
            @Param("vendorName") String vendorName,
            @Param("statuses") List<String> statuses,
            @Param("startMonth") YearMonth startMonth,
            @Param("endMonth") YearMonth endMonth);

    // Alternative: Query for specific month
    @Query("SELECT cia FROM CompanyInvoiceAmount cia " +
            "WHERE cia.vendorName = :vendorName " +
            "AND cia.isActive = true " +
            "AND cia.status IN :statuses " +
            "AND cia.billingMonth = :billingMonth " +
            "ORDER BY cia.id ASC")
    List<CompanyInvoiceAmount> findVendorLedgerByMonth(
            @Param("vendorName") String vendorName,
            @Param("statuses") List<String> statuses,
            @Param("billingMonth") YearMonth billingMonth);

    @Query("SELECT cia FROM CompanyInvoiceAmount cia " +
            "WHERE cia.isActive = true " +
            "AND DATE(cia.purchaseDate) = :date " + // Assuming you have purchaseDate field
            "ORDER BY cia.id DESC")
    List<CompanyInvoiceAmount> findVendorInvoicesByDate(@Param("date") LocalDate date);

    @Query("SELECT " +
            "COALESCE(SUM(CASE " +
            "    WHEN p.status = 'Purchase' THEN " +
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
            "FROM CompanyInvoiceAmount p " +
            "WHERE YEAR(p.purchaseDate) = :year " +
            "AND MONTH(p.purchaseDate) = :month " +
            "AND p.status IN ('Purchase', 'Return') " +
            "AND p.isActive = true")
    BigDecimal getNetPurchaseAmountByMonth(
            @Param("year") int year,
            @Param("month") int month
    );
}
