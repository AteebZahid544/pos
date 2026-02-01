package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "customers")
@Data
public class Customers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "contact_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "is_active")
    private Boolean isActive;



}
