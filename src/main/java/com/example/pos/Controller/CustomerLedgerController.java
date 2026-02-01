package com.example.pos.Controller;

import com.example.pos.DTO.LedgerResponseDto;
import com.example.pos.Service.CustomerLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class CustomerLedgerController {

    private final CustomerLedgerService ledgerService;

    @GetMapping("/customer/{customerName}")
    public LedgerResponseDto getCustomerLedger(
            @PathVariable String customerName,
            @RequestParam(required = false) String startMonth, // Format: "2024-01"
            @RequestParam(required = false) String endMonth) {

        YearMonth start = startMonth != null ? YearMonth.parse(startMonth) : null;
        YearMonth end = endMonth != null ? YearMonth.parse(endMonth) : null;

        return ledgerService.getCustomerLedger(customerName, start, end);
    }

    @GetMapping("/customer/{customerName}/month/{billingMonth}")
    public LedgerResponseDto getCustomerLedgerForMonth(
            @PathVariable String customerName,
            @PathVariable String billingMonth) {

        YearMonth month = YearMonth.parse(billingMonth);
        return ledgerService.getCustomerLedgerForMonth(customerName, month);
    }

    @GetMapping("/vendor/{vendorName}")
    public LedgerResponseDto getVendorLedger(
            @PathVariable String vendorName,
            @RequestParam(required = false) String startMonth, // Format: "2024-01"
            @RequestParam(required = false) String endMonth) {

        YearMonth start = startMonth != null ? YearMonth.parse(startMonth) : null;
        YearMonth end = endMonth != null ? YearMonth.parse(endMonth) : null;

        return ledgerService.getVendorLedger(vendorName, start, end);
    }

    @GetMapping("/vendor/{vendorName}/month/{billingMonth}")
    public LedgerResponseDto getVendorLedgerForMonth(
            @PathVariable String vendorName,
            @PathVariable String billingMonth) {

        YearMonth month = YearMonth.parse(billingMonth);
        return ledgerService.getVendorLedgerForMonth(vendorName, month);
    }
}
