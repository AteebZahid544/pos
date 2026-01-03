package com.example.pos.Controller;

import com.example.pos.Service.CustomerLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class CustomerLedgerController {

    private final CustomerLedgerService ledgerService;

    @GetMapping("/customer/{customerName}")
    public ResponseEntity<?> getLedger(@PathVariable String customerName) {
        return ResponseEntity.ok(
                ledgerService.getCustomerLedger(customerName)
        );
    }

    @GetMapping("/vendor/{vendorName}")
    public ResponseEntity<?> getVendorLedger(@PathVariable String vendorName) {
        return ResponseEntity.ok(
                ledgerService.getVendorLedger(vendorName)
        );
    }
}
