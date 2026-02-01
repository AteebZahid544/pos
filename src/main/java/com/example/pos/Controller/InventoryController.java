package com.example.pos.Controller;

import com.example.pos.DTO.MonthlySummaryDto;
import com.example.pos.Service.InventoryService;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;


    @GetMapping("/grouped-by-category")
    public Status getInventoryGroupedByCategory(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String month) { // Format: "2026-01"

        YearMonth yearMonth = null;
        if (month != null && !month.trim().isEmpty()) {
            try {
                yearMonth = YearMonth.parse(month);
            } catch (Exception e) {
                return new Status(StatusMessage.FAILURE, "Invalid month format. Use YYYY-MM");
            }
        }

        return inventoryService.getInventoryGroupedByCategory(category, yearMonth);
    }


    @GetMapping("/grouped-by-customer")
    public Status getCustomerPaymentsGrouped(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return inventoryService.getAllPaymentsGroupedByCustomer(startDate, endDate);
    }

    @GetMapping("/grouped-by-vendor")
    public Status getVendorPaymentsGrouped(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return inventoryService.getAllPaymentsGroupedByVendor(startDate, endDate);
    }

//    @GetMapping("/total")
//    public ResponseEntity<BigDecimal> getTotalInventoryValue() {
//        return ResponseEntity.ok(inventoryService.getInventoryValueWithFallback());
//    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlySummaryDto> getInventoryForMonth(@RequestParam int year,
                                                                  @RequestParam int month) {
        return ResponseEntity.ok(inventoryService.getMonthlySummary(year, month));
    }
}
