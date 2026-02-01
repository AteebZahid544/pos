package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductInvoiceRequest {

    private List<ProductDto> products;
    private BigDecimal invoiceDiscount;
    private BigDecimal invoiceRent;
    private String description;
    private String vendorName;
    private BigDecimal amountPaid;
    private BigDecimal gstPercentage;
    private BigDecimal gstAmount;
    private BigDecimal totalBeforeGst;


}
