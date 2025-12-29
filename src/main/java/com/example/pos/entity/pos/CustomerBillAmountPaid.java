package com.example.pos.entity.pos;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name="Customer_Bill_Amount_Paid")
@Data
public class CustomerBillAmountPaid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "customer_name")
    private String customerName;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month")
    private YearMonth billingMonth;

    @Column(name = "balance")
    private BigDecimal balance;

}
