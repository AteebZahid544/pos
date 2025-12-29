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
    @PutMapping("/updateCategory/{id}")
    public Status updateCategory(@PathVariable int id,@RequestBody CategoryRequestDto dto) {
        return categoryService.updateCategory(id,dto);
    }

    @GetMapping("/getCategories")
    public Status getCategories(@RequestParam(value = "categoryName", required = false) String categoryName) {
        return categoryService.getCategories(categoryName);
    }

    @DeleteMapping("/deleteCategories/{id}")
    public Status deleteCategory(@PathVariable int id) {
        return categoryService.deleteCategoryById(id);
    }
    @PutMapping("/updateProductNameOrPrice/{id}")
    public Status updateProductData(@PathVariable int id,@RequestBody ProductNameDto productNameDto) {
        return categoryService.updateProductNameAndPrice(id,productNameDto);
    }

    @DeleteMapping("/deleteProductNameAndPrice/{id}")
    public Status deleteProductData(@PathVariable int id) {
        return categoryService.deleteProductById(id);
    }
}
