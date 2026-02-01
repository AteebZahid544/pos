package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DailyExpenseDto {
    private String category;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private LocalTime expenseTime;
    private String expenseType;
}