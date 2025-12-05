package com.example.pos.entity.pos;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "Customers_Balance")
@Data
public class CustomersBill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "bill_paid")
    private BigDecimal billPaid;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month")
    private YearMonth billingMonth;


    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "pay_bill_time")
    private LocalDateTime payBillTime;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "bill_time")
    private String billTime;

    @Column(name = "deleted_payment")
    private BigDecimal deletePayment;

    @Column(name = "deleted_payment_time")
    private LocalDateTime deletePaymentTime;
}
