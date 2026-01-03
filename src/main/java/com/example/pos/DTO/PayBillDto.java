package com.example.pos.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayBillDto {
    private int invoiceNumber;
    private String status;
    private BigDecimal newAmountPaid;
    private BigDecimal amountPaid;
    private String vendorName;
    private String customerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private LocalDateTime paymentTime;
}
