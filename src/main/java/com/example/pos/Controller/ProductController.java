package com.example.pos.Controller;

import com.example.pos.DTO.ProductDto;
import com.example.pos.Service.ProductService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping("/stockAdded")
    public Status productAdded(@RequestBody ProductDto productDto) {
        return productService.productAdded(productDto);
    }

    @PutMapping("/productUpdated/{id}")
    public Status productUpdated(@PathVariable int id
            , @RequestBody ProductDto productDto) {
        return productService.productUpdated(id, productDto);
    }

    @GetMapping("/products/by-category-and-name")
    public Status getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productName
    ) {
        return productService.searchProducts(category, productName);
    }


    @DeleteMapping("/deleteProduct/{id}")
    public Status deleteProduct(@PathVariable int id) {
        return productService.deleteRecord(id);
    }

}
