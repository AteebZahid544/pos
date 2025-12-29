package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
public class ProductEntity {
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

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "returned_quantity")
    private Integer returnedQuantity;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name="product_entry_time")
    LocalDateTime productEntryTime;

    @Column(name="record_updated_time")
    LocalDateTime recordUpdatedTime;

    @Column(name="record_deleted_time")
    LocalDateTime recordDeletedTime;

    @Column(name="stock_return_time")
    LocalDateTime returnTime;

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

    @Column(name = "status")
    private String status;

}
