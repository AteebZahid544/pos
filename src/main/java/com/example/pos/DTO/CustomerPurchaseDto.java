package com.example.pos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CustomerPurchaseDto {
    private String customerName;
    private BigDecimal totalPurchase;
}