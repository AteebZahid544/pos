package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DailyPaymentDto {
    private String payerName; // customer/vendor name
    private BigDecimal amountPaid;
    private LocalDateTime paymentTime;
    private int invoiceNumber;
    private String billingMonth;
    private String type; // "CUSTOMER" or "VENDOR"
}