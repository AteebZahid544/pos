package com.example.pos.Controller;

import com.example.pos.DTO.AdvanceSalaryRequestDto;
import com.example.pos.DTO.SalaryPaymentRequestDto;

import com.example.pos.Service.EmployeeSalaryService;
import com.example.pos.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
public class EmployeeSalaryController {

    private final EmployeeSalaryService service;

    @PostMapping("/advance")
    public Status giveAdvance(@RequestBody AdvanceSalaryRequestDto dto) {
        return service.giveAdvance(dto);
    }

    @PostMapping("/pay")
    public Status paySalary(@RequestBody SalaryPaymentRequestDto dto) {
        return service.paySalary(dto);
    }

    @GetMapping("/info")
    public Status getEmployeeSalaryInfo(
            @RequestParam String name,
            @RequestParam String designation
    ) {
        return service.getEmployeeSalaryInfo(name, designation);
    }

    @GetMapping("/details")
    public Status getEmployeeSalaryDetails(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // If dates are not provided, default to a wide range
        if (startDate == null) startDate = LocalDate.of(1900, 1, 1);
        if (endDate == null) endDate = LocalDate.of(3000, 12, 31);

        return service.getEmployeeSalaryDetailView(startDate, endDate);
    }

    @DeleteMapping("/delete/{salaryId}")
    public Status deleteRecord(@PathVariable Long salaryId){
        return  service.deleteSalary(salaryId);
    }
}
