package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vendors")
@Data
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "address")
    private String address;

    @Column(name = "is_active")
    private Boolean isActive;



}
