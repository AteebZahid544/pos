package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InvoiceDto {
    private int invoiceNumber;
    private String vendorName;
    private List<ProductDto> products;
    private List<SalesDto> sales;
    private BigDecimal invoiceTotal;
    private String customerName;

    private BigDecimal invoiceDiscount;
    private BigDecimal invoiceRent;
    private BigDecimal amountPaid;
    private String description;
    private BigDecimal grandTotal;

    // getters & setters
}
