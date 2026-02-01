package com.example.pos.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerPayBillDto {
    private String customerName;
    private BigDecimal amountPaid;
    private int invoiceNumber;
    private String status;
    private BigDecimal newAmountPaid;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private LocalDateTime paymentTime;
}
