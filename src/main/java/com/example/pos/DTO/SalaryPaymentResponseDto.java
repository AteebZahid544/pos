package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SalaryPaymentResponseDto {
    private Long employeeId;
    private BigDecimal baseSalary;
    private BigDecimal deduction;
    private BigDecimal bonus;
    private BigDecimal overtime;
    private BigDecimal totalPaid;
    private String salaryType;
    private String status;
    private LocalDateTime paymentDate;

}
