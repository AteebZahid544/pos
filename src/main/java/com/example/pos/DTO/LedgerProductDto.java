package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LedgerProductDto {
    private String productName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String category;
}
