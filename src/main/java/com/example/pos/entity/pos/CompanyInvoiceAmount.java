package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

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

}
