//package com.example.pos.entity.pos;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Entity
//@Table(name = "invoices")
//@Data
//public class InvoiceEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int id;
//
//    @Column(name = "invoice_number", unique = true)
//    private Integer invoiceNumber;
//
//    @Column(name = "vendor_name")
//    private String vendorName;
//
//    @Column(columnDefinition = "TEXT") // JSON array of products
//    private String productsJson;
//
//    @Column(name = "invoice_entry_time")
//    private LocalDateTime invoiceEntryTime;
//
//}
