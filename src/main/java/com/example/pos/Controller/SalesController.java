package com.example.pos.Controller;

import com.example.pos.DTO.*;
import com.example.pos.Service.SalesService;
import com.example.pos.repo.pos.SalesRepo;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/product")
public class SalesController {
    @Autowired
    private SalesService salesService;

    @Autowired
    private SalesRepo salesRepo;

    @PostMapping("/sell")
    public Status productSell(@RequestBody SaleInvoiceRequest salesDto,@RequestParam String status){
        return salesService.productSold(
                salesDto.getProducts(),
                salesDto.getInvoiceDiscount(),
                salesDto.getInvoiceRent(),
                salesDto.getDescription(),
                salesDto.getCustomerName(),
                salesDto.getPayBill(),
                status
        );
    }
    @GetMapping("/sales/by-category-and-name")
    public Status getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productName,
            @RequestParam String status
    ) {
        return salesService.searchProducts(category, productName,status);
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
                status
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
