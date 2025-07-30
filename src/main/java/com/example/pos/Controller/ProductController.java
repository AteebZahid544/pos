package com.example.pos.Controller;

import com.example.pos.DTO.ProductDto;
import com.example.pos.Service.ProductService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping("/categoryAdded")
    public Status productAdded(@RequestBody ProductDto productDto) {
        return productService.productAdded(productDto);
    }

    @PutMapping("/categoryUpdated/{category}")
    public Status productUpdated(@PathVariable String category, @RequestBody ProductDto productDto) {
        return productService.productUpdated(category, productDto);
    }

    @GetMapping("/getProducts")
    public Status getProducts(@RequestParam(required = false) String category) {
        return productService.getProductsByCategory(category);
    }


    @DeleteMapping("/deleteProduct/{id}")
    public Status deleteProduct(@PathVariable int id){
        return productService.deleteRecord(id);
    }

    @PostMapping("/companyPayBill")
    public Status companyPayBill(@RequestParam String vendorName,@RequestParam int amount) {
        return productService.payVendorBill(vendorName,amount);
    }
}
