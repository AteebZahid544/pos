package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // e.g., "DAILY" or "MONTHLY"

    @Column(name = "is_active")
    private Boolean isActive = true;
}
