package com.example.pos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class CategoryRequestDto {
    private Long id;
    private String categoryName;
    private List<ProductNameDto> products;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    public List<ProductNameDto> getProducts() {
        return products;
    }
    public void setProducts(List<ProductNameDto> products) {
        this.products = products;
    }
}
