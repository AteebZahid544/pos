package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RevertDto {
    private String productName;
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal size;
    private BigDecimal ktae;
    private BigDecimal gram;
    private BigDecimal generalDiscount;
    private BigDecimal rent;
}
