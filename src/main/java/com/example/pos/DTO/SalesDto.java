package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesDto {

    private String category;

    private String product;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal discount;

    private BigDecimal totalAmount;

    private String customerName;

    private BigDecimal amountPaid;

}
