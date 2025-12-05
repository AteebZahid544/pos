package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerBalanceResponseDto {
    private String customerName;
    private BigDecimal customerBalance;
}
