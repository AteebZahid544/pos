package com.example.pos.entity.pos;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Data
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @Column(name = "category")
    private String category;

    @Column(name = "total_price")
    public BigDecimal totalPrice;

    @Column(name = "product_name")
    public String productName;

    @Column(name = "size")
    private BigDecimal size;

    @Column(name = "ktae")
    private BigDecimal ktae;

    @Column(name = "gram")
    private BigDecimal gram;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "added_month")
    private YearMonth addedMonth; // format: 2026-01


}
