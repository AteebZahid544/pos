package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale")
@Data
public class SalesEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "category")
    private String category;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "purchase_price") // ✅ NEW: Purchase price at time of sale
    private BigDecimal purchasePrice;

    @Column(name = "purchase_cost") // ✅ NEW: Purchase price at time of sale
    private BigDecimal purchaseCost;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name="sale_entry_time")
    LocalDateTime saleEntryTime;

    @Column(name="record_updated_time")
    LocalDateTime recordUpdatedTime;

    @Column(name="record_deleted_time")
    LocalDateTime recordDeletedTime;

    @Column(name = "is_Active")
    private Boolean isActive;

    @Column(name = "size")
    private BigDecimal size;

    @Column(name = "ktae")
    private BigDecimal ktae;

    @Column(name = "gram")
    private BigDecimal gram;

    @Column(name = "invoice_number")
    private Integer invoiceNumber;

    @Column(name = "returned_quantity")
    private Integer returnedQuantity;

    @Column(name = "return_time")
    private LocalDateTime returnTime;

    @Column(name = "status")
    private String status;


}
