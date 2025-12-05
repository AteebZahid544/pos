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

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name="product_entry_time")
    LocalDateTime productEntryTime;

    @Column(name="record_updated_time")
    LocalDateTime recordUpdatedTime;

    @Column(name="record_deleted_time")
    LocalDateTime recordDeletedTime;

    @Column(name = "is_Active")
    private Boolean isActive;

}
