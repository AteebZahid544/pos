package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDto {

    private String productName;
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private String vendorName;
    private BigDecimal totalPrice;
    private BigDecimal payBill;
    private LocalDateTime productEntryTime;
    private LocalDateTime recordUpdatedTime;
    private Boolean isActive;
}
