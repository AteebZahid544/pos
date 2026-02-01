package com.example.pos.DTO;

import com.example.pos.entity.pos.Category;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductNameResponseDto {
    private String productName;
    private BigDecimal purchasePrice;
    private BigDecimal sellPrice;
    private String category;
}
