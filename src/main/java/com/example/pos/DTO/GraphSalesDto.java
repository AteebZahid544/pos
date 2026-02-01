package com.example.pos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GraphSalesDto {
    private String productName;
    private BigDecimal totalSale;
}