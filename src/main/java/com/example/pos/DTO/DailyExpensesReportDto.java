package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class DailyExpensesReportDto {
    private LocalDate date;
    private List<DailyExpenseDto> dailyExpenses;
    private BigDecimal totalDailyExpenses;
    private Integer totalExpenses;
    private Map<String, BigDecimal> expensesByCategory; // Optional: breakdown by category
}