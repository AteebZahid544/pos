package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Data
public class LedgerInvoiceDto {

    private Integer invoiceNumber;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String status;
    private LocalDateTime purchaseRecordTime;
    private LocalDateTime saleRecordTime;
    private LocalDateTime returnRecordTime;
    private YearMonth billingMonth;
    private BigDecimal gstPercentage;
    private BigDecimal gstAmount;
    private BigDecimal totalBeforeGst;


    private List<LedgerProductDto> products;
}
