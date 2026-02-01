package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "production_step")
public class ProductStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="step_name")
    private String stepName;

    @Column(name="step_order")

    private Integer stepOrder;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductManufacture product;

    // Getters and setters
}
