package com.example.pos.Controller;

import com.example.pos.DTO.ProductInvoiceRequest;
import com.example.pos.DTO.StockRevertDto;
import com.example.pos.Service.ProductService;

import com.example.pos.repo.pos.ProductRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepo productRepo;

    @PostMapping(value = "/stockAdded", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Status productAdded(
            @RequestParam("request") String requestJson,  // JSON as string
            @RequestParam String status,
            @RequestParam(value = "invoiceImage", required = false) MultipartFile invoiceImage
    ) throws IOException {

        // Deserialize manually
        ObjectMapper mapper = new ObjectMapper();
        ProductInvoiceRequest request = mapper.readValue(requestJson, ProductInvoiceRequest.class);

        Status response = productService.productAdded(
                request.getProducts(),
                request.getInvoiceDiscount(),
                request.getInvoiceRent(),
                request.getDescription(),
                request.getVendorName(),
                request.getAmountPaid(),
                status,
                request.getGstPercentage()
        );

        if (response.getCode() == StatusMessage.SUCCESS.getId() && invoiceImage != null) {
            productService.saveInvoiceImage(
                    request.getProducts().get(0).getInvoiceNumber(),
                    status,
                    invoiceImage
            );
        }

        return response;
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
                status,
                request.getGstPercentage(),
                request.getGstAmount(),
                request.getTotalBeforeGst()
        );
    }

    @GetMapping("/products/by-category-and-name")
    public Status getProducts(
            @RequestParam String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return productService.searchProducts(status, category, productName, startDate, endDate);
    }

    @GetMapping("/invoice-image")
    public ResponseEntity<Resource> getInvoiceImage(@RequestParam String invoiceImagePath) throws IOException {
        // Decode URL-encoded path
        String decodedPath = URLDecoder.decode(invoiceImagePath, StandardCharsets.UTF_8);

        File file = new File(decodedPath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        // Determine content type
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
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

    @PostMapping(value = "/returnStock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Status productAdded(
            @RequestParam("request") String requestJson,  // JSON as string
            @RequestParam(value = "invoiceImage", required = false) MultipartFile invoiceImage
    ) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        StockRevertDto request = mapper.readValue(requestJson, StockRevertDto.class);

        Status response= productService.revertStock(
                request.getInvoiceNumber(),
                request.getVendorName(),
                request.getInvoiceDiscount(),
                request.getInvoiceRent(),
                request.getDescription(),
                request.getStock(),
                request.getGstPercentage()
        );
        if (response.getCode() == StatusMessage.SUCCESS.getId() && invoiceImage != null) {
            productService.saveInvoiceImage(
                    request.getInvoiceNumber(),
                    "Return",
                    invoiceImage
            );
        }

        return response;
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
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return productService.searchProducts(status,category, productName,startDate,endDate);
    }

//    @GetMapping("/monthly")
//    public ResponseEntity<BigDecimal> getCompanyBalanceForMonth(@RequestParam int year,
//                                                           @RequestParam int month) {
//        return ResponseEntity.ok(productService.getCompanyBalanceForMonth(year, month));
//    }
}


