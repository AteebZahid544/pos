package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleInvoiceRequest {

    private List<SalesDto> products;
    private BigDecimal invoiceDiscount;
    private BigDecimal invoiceRent;
    private String description;
    private String customerName;
    private BigDecimal payBill;


}
