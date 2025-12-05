package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompanyPayBillDto {
    private String vendorName;
    private BigDecimal amount;
}
