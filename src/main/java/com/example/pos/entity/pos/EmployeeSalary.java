package com.example.pos.entity.pos;

import com.example.pos.util.SalaryStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "employee_salary")
@Data
public class EmployeeSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "base_salary")
    private BigDecimal baseSalary;

    @Column(name = "bonus")
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "overtime")
    private BigDecimal overtime = BigDecimal.ZERO;

    @Column(name = "deduction")
    private BigDecimal deduction = BigDecimal.ZERO;

    @Column(name = "total_paid")
    private BigDecimal totalPaid;

    @Column(name = "status")
    private String status; // PAID / UNPAID

    @Column(name = "salary_month")
    private LocalDate salaryMonth;

    @Column(name = "salary_type")
    private String salaryType;

    @Column(name = "advance_given")

    private BigDecimal advanceGiven;

    @Column(name = "advance_adjusted")
    private BigDecimal advanceAdjusted;

    @Column(name = "remaining_advance")
    private BigDecimal remainingAdvance;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "paid_on")
    private LocalDateTime paidOn;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "salary_date")
    private LocalDate salaryDate;

}

