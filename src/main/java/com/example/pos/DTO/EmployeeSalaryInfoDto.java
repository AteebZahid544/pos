package com.example.pos.DTO;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EmployeeSalaryInfoDto {
    private Long employeeId;
    private String name;
    private String designation;
    private BigDecimal baseSalary;
    private BigDecimal pendingAdvance;
}
