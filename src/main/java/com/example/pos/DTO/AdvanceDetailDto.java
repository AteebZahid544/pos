package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdvanceDetailDto {

    private LocalDateTime advanceDate;
    private BigDecimal advanceAmount;
    private BigDecimal remainingAdvance;
    private String status; // PAID or ADJUSTED

}
