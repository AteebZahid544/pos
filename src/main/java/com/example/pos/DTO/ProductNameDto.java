package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductNameDto {

    private Long id;
    private String productName;
    private BigDecimal productPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public BigDecimal getProductPrice() {
        return productPrice;
    }
    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }
}
