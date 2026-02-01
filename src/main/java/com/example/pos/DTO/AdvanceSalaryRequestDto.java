package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdvanceSalaryRequestDto {

    private Long employeeId;
    private BigDecimal amount;
    private String remarks;
    private LocalDateTime paidOn;
}
