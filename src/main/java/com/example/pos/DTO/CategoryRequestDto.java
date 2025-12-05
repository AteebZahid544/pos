package com.example.pos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class CategoryRequestDto {
    private Long id;
    private String categoryName;
    private List<ProductNameDto> products;

}
