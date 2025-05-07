package com.example.pos.DTO;

import java.math.BigDecimal;

public class ProductDto {
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private boolean isActive;


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

}
