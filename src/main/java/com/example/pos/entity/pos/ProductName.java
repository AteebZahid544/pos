package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_name")
@Data
@ToString(exclude = "category")

public class ProductName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @Column(name = "sell_price")
    private BigDecimal sellPrice;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "deleted_by")
    private LocalDateTime deletedBy;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

}

