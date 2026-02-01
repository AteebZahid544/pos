package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductNameDto {

    private int id;
    private String productName;
    private BigDecimal purchasePrice;
    private BigDecimal sellPrice;

}
