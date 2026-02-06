package com.example.pos.entity.pos;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
@Entity
@Table(name ="company_invoice_amount" )
public class CompanyInvoiceAmount {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "invoice_number")
    private Integer invoiceNumber;

    @Column(name="grand_total")
    private BigDecimal grandTotal;

    @Column(name = "amount_Paid")
    private BigDecimal amountPaid;

    @Column(name="discount")
    private BigDecimal discount;

    @Column(name="rent")
    private BigDecimal rent;

    @Column(name = "description")
    private String description;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "Status")
    private String status;

    @Column(name = "invoice_image")
    private String invoiceImagePath;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage; // e.g., 18.00 for 18%

    @Column(name = "gst_amount", precision = 19, scale = 2)
    private BigDecimal gstAmount; // Calculated GST amount

    @Column(name = "total_before_gst", precision = 19, scale = 2)
    private BigDecimal totalBeforeGst; // Total before applying GST

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month")
    private YearMonth billingMonth;

    @Column(name = "invoice_date")
    private LocalDateTime invoiceDate;

    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;
}
