package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyInvoiceProductDto {
    private String productName;
    private String category;
    private Integer quantity;
    private BigDecimal totalPrice;
}