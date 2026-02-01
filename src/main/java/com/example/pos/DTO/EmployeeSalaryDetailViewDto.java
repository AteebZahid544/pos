package com.example.pos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeSalaryDetailViewDto {

    private Long employeeId;
    private String employeeName;
    private String designation;

    private List<EmployeeMonthlySalaryDetailDto> monthlyDetails;
}
