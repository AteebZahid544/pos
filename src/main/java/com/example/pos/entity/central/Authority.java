package com.example.pos.entity.central;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "authority")
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer authorityId;

    @Column(nullable = false, unique = true)
    private String authorityName; // PURCHASE, SALE, PRODUCTION

    public Authority() {}
    public Authority(String authorityName) { this.authorityName = authorityName; }

}


