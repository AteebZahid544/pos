package com.example.pos.Controller;

import com.example.pos.DTO.CategoryRequestDto;
import com.example.pos.DTO.ProductNameDto;
import com.example.pos.Service.CategoryService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping("/addCategoryWithProducts")
    public Status addCategory(@RequestBody CategoryRequestDto dto) {
        return categoryService.saveCategoryWithProducts(dto);
    }
    @PutMapping("/updateCategory")
    public Status updateCategory(@RequestBody CategoryRequestDto dto) {
        return categoryService.updateCategoryWithProducts(dto);
    }

    @GetMapping("/getCategories")
    public Status getCategories(@RequestParam(value = "categoryName", required = false) String categoryName) {
        return categoryService.getCategories(categoryName);
    }

    @DeleteMapping("/deleteCategories")
    public Status deleteCategory(@RequestParam("categoryName") String categoryName) {
        return categoryService.deleteCategoryByName(categoryName);
    }
    @PutMapping("/updateProductData")
    public Status updateProductData(@RequestBody ProductNameDto productNameDto) {
        return categoryService.updateProductNameAndPrice(productNameDto);
    }

    @DeleteMapping("/deleteProductData")
    public Status deleteProductData(@RequestBody ProductNameDto productNameDto) {
        return categoryService.deleteProductByName(productNameDto);
    }
}
