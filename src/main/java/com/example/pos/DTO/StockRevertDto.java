package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StockRevertDto {

    private int invoiceNumber;
    private String vendorName;
    private BigDecimal invoiceDiscount;
    private BigDecimal invoiceRent;
    private String description;
   List<RevertDto> stock;
}
