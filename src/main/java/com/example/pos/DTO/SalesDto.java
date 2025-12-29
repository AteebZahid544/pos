package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesDto {

    private String productName;
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private LocalDateTime saleEntryTime;
    private LocalDateTime recordUpdatedTime;
    private Boolean isActive;
    private BigDecimal size;
    private BigDecimal ktae;
    private BigDecimal gram;
    private BigDecimal generalDiscount;
    private Integer invoiceNumber;
    private BigDecimal rent;
    private String description;
    private BigDecimal grandTotal;
    private Integer returnedQuantity;
    private String returnTime;
}
