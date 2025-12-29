package com.example.pos.entity.pos;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Entity
@Table(name ="customer_invoice_record" )
public class CustomerInvoiceRecord {
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

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month")
    private YearMonth billingMonth;

    @Column(name = "Status")
    private String status;
}
