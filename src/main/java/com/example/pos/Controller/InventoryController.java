package com.example.pos.Controller;

import com.example.pos.Service.InventoryService;
import com.example.pos.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;


    @GetMapping("/grouped-by-category")
    public Status getInventoryByCategory(
            @RequestParam(required = false) String category
    ) {
        return inventoryService.getInventoryGroupedByCategory(category);
    }


    @GetMapping("/grouped-by-customer")
    public Status getAllCustomerPayments() {
        return inventoryService
                .getAllPaymentsGroupedByCustomer();
    }

    @GetMapping("/grouped-by-vendor")
    public Status getAllCompanyPayments() {
        return inventoryService
                .getAllPaymentsGroupedByVendor();
    }
}
