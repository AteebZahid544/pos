package com.example.pos.DTO;

import lombok.Data;

@Data
public class ExpenseCategoryDto {
    private Long id;
    private String name;
    private String type; // "DAILY" or "MONTHLY"
}
