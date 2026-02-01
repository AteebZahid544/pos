package com.example.pos.entity.pos;

import com.example.pos.util.SalaryType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employ_name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "designation")
    private String designation;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "salary")
    private BigDecimal salary;

    @Column(name = "id_card_image")
    private String idCardImage; // image path or URL

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type")
    private SalaryType salaryType;


    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

