package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "product_manufacture")
public class ProductManufacture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductStep> steps;

}
