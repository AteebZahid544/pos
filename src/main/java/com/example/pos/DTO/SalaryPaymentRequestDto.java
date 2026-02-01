package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryPaymentRequestDto {

    private Long employeeId;

    private BigDecimal bonus;
    private BigDecimal overTime;
    private BigDecimal deduction;
    private BigDecimal advanceAdjustment;
    private String salaryType;
    private LocalDate salaryDate;
}
