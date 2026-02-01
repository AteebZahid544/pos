package com.example.pos.Controller;

import com.example.pos.DTO.*;
import com.example.pos.Service.SalesService;
import com.example.pos.repo.pos.SalesRepo;
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
public class SalesController {
    @Autowired
    private SalesService salesService;

    @Autowired
    private SalesRepo salesRepo;

    @PostMapping(value= "/sell", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Status productSell(
            @RequestParam("request") String requestJson,  // JSON as string
            @RequestParam String status,
            @RequestParam(value = "invoiceImage", required = false) MultipartFile invoiceImage
    ) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        SaleInvoiceRequest request = mapper.readValue(requestJson, SaleInvoiceRequest.class);

        Status response= salesService.productSold(
                request.getProducts(),
                request.getInvoiceDiscount(),
                request.getInvoiceRent(),
                request.getDescription(),
                request.getCustomerName(),
                request.getPayBill(),
                status,
                request.getGstPercentage()
        );

        if (response.getCode() == StatusMessage.SUCCESS.getId() && invoiceImage != null) {
            salesService.saveInvoiceImage(
                    request.getProducts().get(0).getInvoiceNumber(),
                    status,
                    invoiceImage
            );
        }

        return response;
    }


    @GetMapping("/sales/by-category-and-name")
    public Status getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productName,
            @RequestParam String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return salesService.searchProducts(category, productName,status,startDate,endDate);
    }

    @GetMapping("/sale/invoice-image")
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


    @PutMapping("/saleUpdated/{invoiceNumber}")
    public Status updateStock(@PathVariable int invoiceNumber,@RequestBody SaleInvoiceRequest request, @RequestParam String status) {
        return salesService.updateSaleEntry(invoiceNumber,
                request.getProducts(),
                request.getInvoiceDiscount(),
                request.getInvoiceRent(),
                request.getDescription(),
                request.getCustomerName(),
                request.getPayBill(),
                status,
                request.getGstPercentage(),
                request.getGstAmount(),
                request.getTotalBeforeGst()
        );
    }

    @DeleteMapping("/delete/sale/{invoiceNumber}")
    public Status deleteSale(@PathVariable Integer invoiceNumber, @RequestParam String status) {
        return salesService.deleteRecord(invoiceNumber, status);
    }

//    @GetMapping("/get-customer-balance")
//    public Status getBalance(@RequestParam(required = false) String customerName) {
//        return salesService.getCustomerBalance(customerName);
//    }
//
//    @GetMapping("/customer-ledger")
//    public Status getCustomerLedger(@RequestParam String customerName) {
//        return salesService.getCustomerLedger(customerName);
//    }

    @PostMapping("/customer-name")
    public Status addCustomer(@RequestBody CustomerRequestDTO customerRequestDTO){
        return salesService.addCustomer(customerRequestDTO);
    }

    @GetMapping("/getAllCustomers")
    public Status getAllCustomers() {
        return salesService.getAllCustomers();
    }

    @PutMapping("/updateCustomer/{id}")
    public Status updateCustomer(@PathVariable Long id,
                               @RequestBody CustomerRequestDTO dto) {
        return salesService.updateCustomer(id, dto);
    }

    @DeleteMapping("/deleteCustomer/{id}")
    public Status deleteCustomer(@PathVariable Long id) {
        return salesService.deleteCustomer(id);
    }
    @PostMapping("/vendor-name")
    public Status addVendor(@RequestBody VendorRequestDTO vendorRequestDTO){
        return salesService.addVendor(vendorRequestDTO);
    }

    @GetMapping("/getAll")
    public Status getAllVendors() {
        return salesService.getAllVendors();
    }

    @PutMapping("/updateVendor/{id}")
    public Status updateVendor(@PathVariable Long id,
                               @RequestBody VendorRequestDTO dto) {
        return salesService.updateVendor(id, dto);
    }

    @DeleteMapping("/deleteVendor/{id}")
    public Status deleteVendor(@PathVariable Long id) {
        return salesService.deleteVendor(id);
    }

    @GetMapping("/sale/next-invoice")
    public ResponseEntity<Map<String, Integer>> getNextInvoice() {
        Integer lastInvoice = salesRepo.findMaxInvoiceNumberByStatus("Sale");
        int nextInvoice = (lastInvoice != null ? lastInvoice : 0) + 1;
        return ResponseEntity.ok(Map.of("nextInvoice", nextInvoice));
    }
    @GetMapping("/sale-return/next-invoice")
    public ResponseEntity<Map<String, Integer>> getReturnNextInvoice() {
        Integer lastInvoice = salesRepo.findMaxInvoiceNumberByStatus("Return");
        int nextInvoice = (lastInvoice != null ? lastInvoice : 0) + 1;
        return ResponseEntity.ok(Map.of("nextInvoice", nextInvoice));
    }
}
