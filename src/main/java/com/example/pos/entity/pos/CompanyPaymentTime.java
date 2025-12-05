package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_payment_time")
@Data
public class CompanyPaymentTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "amount_paid")
    private BigDecimal amountPaid;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;
}
