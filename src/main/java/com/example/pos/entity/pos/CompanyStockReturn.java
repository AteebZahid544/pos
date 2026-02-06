//package com.example.pos.entity.pos;
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "company_stock_return")
//@Data
//public class CompanyStockReturn {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int id;
//
//    @Column(name = "invoice_number")
//    private int invoiceNumber;
//
//    @Column(name = "vendor_name")
//    private String vendorName;
//
//    @Column(name = "product_name")
//    private String productName;
//
//    @Column(name = "category")
//    private String category;
//
//    @Column(name = "returned_quantity")
//    private int returnedQuantity;
//
//    @Column(name = "price")
//    private BigDecimal price;
//
//    @Column(name = "total_amount")
//    private BigDecimal totalAmount;
//
//    @Column(name = "size")
//    private BigDecimal size;
//
//    @Column(name = "gram")
//    private BigDecimal gram;
//
//    @Column(name = "ktae")
//    private BigDecimal ktae;
//
//    @Column(name = "return_time")
//    private LocalDateTime returnTime;
//}
