package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DailyInvoiceDto {
    private Integer invoiceNumber;
    private String customerOrVendorName;
    private String type; // "Customer" or "Vendor"
    private String status; // "Sale", "Return", "Purchase"
    private BigDecimal grandTotal;
    private BigDecimal gstAmount;
    private BigDecimal totalBeforeGst;
    private BigDecimal amountPaid;
    private LocalDateTime invoiceTime;
    private List<DailyInvoiceProductDto> products;



}