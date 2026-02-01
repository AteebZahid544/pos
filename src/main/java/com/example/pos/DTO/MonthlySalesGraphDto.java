package com.example.pos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlySalesGraphDto {
    private String month;   // JAN, FEB, MAR

    private BigDecimal totalSale;
}
