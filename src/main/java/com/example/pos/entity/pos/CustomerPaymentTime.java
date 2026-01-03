package com.example.pos.entity.pos;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "customer_payment_time")
@Data
public class CustomerPaymentTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "amount_paid")
    private BigDecimal amountPaid;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month")
    private YearMonth billingMonth;

    @Column(name = "invoice_number")
    private int invoiceNumber;

    @Column(name = "is_active")
    private Boolean isActive;
}


