package com.example.pos.entity.central;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "authentication")
@Data
public class Authentication {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "username")
    private String username;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "database_name")
    private String databaseName;

    @Column(name ="address")
    private String address;

    @Column(name ="role")
    private String role;
}
