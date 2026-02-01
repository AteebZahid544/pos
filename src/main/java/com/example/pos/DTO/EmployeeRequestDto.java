package com.example.pos.DTO;

import com.example.pos.util.SalaryType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmployeeRequestDto {

    private Long id;
    private String name;
    private String address;
    private String designation;
    private String contactNumber;
    private BigDecimal salary;
    private SalaryType salaryType;
}
