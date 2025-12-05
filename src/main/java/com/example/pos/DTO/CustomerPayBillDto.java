package com.example.pos.DTO;

import lombok.Data;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;

import java.math.BigDecimal;

@Data
public class CustomerPayBillDto {
    private String customerName;
    private BigDecimal amount;
}
