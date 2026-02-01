package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmployeeMonthlySalaryDetailDto {

    private LocalDate salaryMonth;

    private BigDecimal baseSalary;

    private int totalAdvancesCount;
    private BigDecimal totalAdvanceAmount;

    private BigDecimal bonus;
    private BigDecimal overTime;
    private BigDecimal deduction;
    private BigDecimal advanceAdjustment;
    private LocalDateTime salaryPaidOn;

    private BigDecimal netSalary;

    private List<AdvanceDetailDto> advances;

    private BigDecimal totalPaid; // New field
    private BigDecimal grossSalary; // New field
    private BigDecimal pendingAdvance; // New field
}
