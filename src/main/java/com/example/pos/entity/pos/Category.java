package com.example.pos.entity.pos;


import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "category")
@Data
@ToString(exclude = "products")

public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name= "category_name")
    private String categoryName;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    private List<ProductName> products;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "deleted_by")
    private LocalDateTime deletedBy;

}

