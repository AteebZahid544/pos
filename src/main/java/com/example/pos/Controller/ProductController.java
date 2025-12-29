package com.example.pos.Controller;

import com.example.pos.DTO.ProductDto;
import com.example.pos.DTO.ProductInvoiceRequest;
import com.example.pos.DTO.StockRevertDto;
import com.example.pos.Service.ProductService;
import com.example.pos.repo.pos.CompanyStockReturnRepo;
import com.example.pos.repo.pos.ProductRepo;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CompanyStockReturnRepo companyStockReturnRepo;

    @PostMapping("/stockAdded")
    public Status productAdded(@RequestBody ProductInvoiceRequest request, @RequestParam String status) {
        return productService.productAdded(
                request.getProducts(),
                request.getInvoiceDiscount(),
                request.getInvoiceRent(),
                request.getDescription(),
                request.getVendorName(),
                request.getAmountPaid(),
                status
        );
    }


    @PutMapping("/productUpdated/{invoiceNumber}")
    public Status updateStock(@PathVariable int invoiceNumber, @RequestBody ProductInvoiceRequest request,@RequestParam("status") String status) {
        return productService.updateStock(invoiceNumber,
                request.getProducts(),
                request.getInvoiceDiscount(),
                request.getInvoiceRent(),
                request.getDescription(),
                request.getVendorName(),
                request.getAmountPaid(),
                status
        );
    }

    @GetMapping("/products/by-category-and-name")
    public Status getProducts(
            @RequestParam String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productName
    ) {
        return productService.searchProducts(status,category, productName);
    }


    @DeleteMapping("/deleteProduct/{invoiceNumber}")
    public Status deleteProduct(@PathVariable Integer invoiceNumber,
                                @RequestParam String status) {
        return productService.deleteRecord(invoiceNumber,status);
    }

    @GetMapping("/names-prices")
    public Status getAllProductNamesAndPrices() {
        return productService.getAllProductNamesAndPrices();
    }

    @GetMapping("/purchase/next-invoice")
    public ResponseEntity<Map<String, Integer>> getNextInvoice() {
        Integer lastInvoice = productRepo.findMaxInvoiceNumberByStatus("Purchase"); // e.g., SELECT MAX(invoiceNumber)
        int nextInvoice = (lastInvoice != null ? lastInvoice : 0) + 1;
        return ResponseEntity.ok(Map.of("nextInvoice", nextInvoice));
    }

    @PostMapping("/return-stock")
    public Status returnStock(@RequestBody StockRevertDto stockRevertDto) {
        return productService.revertStock(
                stockRevertDto.getInvoiceNumber(),
                stockRevertDto.getVendorName(),
                stockRevertDto.getInvoiceDiscount(),
                stockRevertDto.getInvoiceRent(),
                stockRevertDto.getDescription(),
                stockRevertDto.getStock()
        );
    }

    @GetMapping("/purchaseStockRevert/next-invoice")
    public ResponseEntity<Map<String, Integer>> getStockRevertNextInvoice() {
        Integer lastInvoice = productRepo.findMaxInvoiceNumberByStatus("Return"); // e.g., SELECT MAX(invoiceNumber)
        int nextInvoice = (lastInvoice != null ? lastInvoice : 0) + 1;
        return ResponseEntity.ok(Map.of("nextInvoice", nextInvoice));
    }

    @GetMapping("/returns/view")
    public Status getReturnView(
            @RequestParam String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productName
    ) {
        return productService.searchProducts(status,category, productName);
    }

}


