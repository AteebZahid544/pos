package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "expenses")
public class Expenses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "amount")

    private double amount;

    @Column(name = "expense_date")

    private LocalDate expenseDate;

    @Column(name = "expense_time")

    private LocalTime expenseTime;

    @Column(name = "is_active")

    private Boolean isActive;

    @Column(name = "expense_type")
    private String expenseType; // "DAILY" or "MONTHLY"

}
