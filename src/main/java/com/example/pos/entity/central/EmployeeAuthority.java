package com.example.pos.entity.central;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "employee_authority")
public class EmployeeAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id",nullable = false)
    private String employeeId;  // FK to authentication table

    @Column(name = "authority_id",nullable = false)
    private Integer authorityId; // FK to Authority table

    public EmployeeAuthority(String employeeId, Integer authorityId) {
        this.employeeId = employeeId;
        this.authorityId = authorityId;
    }

}
