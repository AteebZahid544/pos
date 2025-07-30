package com.example.pos.entity;

import com.example.pos.config.YearMonthAttributeConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name="Company_Bill_Amount_Paid")
public class CompanyBillAmountPaid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "amount_paid")
    private Integer amountPaid;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month")
    private YearMonth billingMonth;


    @Column(name = "balance")
    private Integer balance;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public Integer getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Integer amountPaid) {
        this.amountPaid = amountPaid;
    }

    public YearMonth getAmountPaidTime() {
        return billingMonth;
    }

    public void setBillingMonth(YearMonth billingMonth) {
        this.billingMonth = billingMonth;
    }
}
