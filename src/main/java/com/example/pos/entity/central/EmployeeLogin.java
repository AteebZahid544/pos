package com.example.pos.entity.central;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "employee_login")
public class EmployeeLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "phone_number",nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String ownerName; // Owner ke under hai ye employee

    @Column(nullable = false)
    private String tenantSchema;

    // Optional: Authorities
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "employee_authority",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private List<Authority> authorities;

    // Constructors
    public EmployeeLogin() {}

    public EmployeeLogin(String username,String ownerName, String tenantSchema) {
        this.username = username;
//        this.password = password;
        this.ownerName = ownerName;
        this.tenantSchema = tenantSchema;
    }
}
