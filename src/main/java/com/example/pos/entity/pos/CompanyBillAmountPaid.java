package com.example.pos.entity.pos;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name="Company_Bill_Amount_Paid")
@Data
public class CompanyBillAmountPaid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "bill_paid")
    private BigDecimal billPaid;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month")
    private YearMonth billingMonth;


    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name="pay_bill_time")
    LocalDateTime payBillTime;
}
