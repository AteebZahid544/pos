package com.example.pos.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ExpenseDto {
    private String category;
    private String description;
    private double amount;
    private LocalDate expenseDate;
    private LocalTime expenseTime;
    private String expenseType;

}
