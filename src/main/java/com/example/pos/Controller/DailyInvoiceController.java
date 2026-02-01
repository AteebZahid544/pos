package com.example.pos.Controller;

import com.example.pos.DTO.*;
import com.example.pos.Service.DailyInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/daily-invoices")
@CrossOrigin(origins = "*")
public class DailyInvoiceController {
    
    @Autowired
    private DailyInvoiceService dailyInvoiceService;

    @GetMapping("/date/{date}")
    public ResponseEntity<DailyInvoiceReportDto> getDailyInvoices(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            DailyInvoiceReportDto report = dailyInvoiceService.getDailyInvoices(date);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/today")
    public ResponseEntity<DailyInvoiceReportDto> getTodayInvoices() {
        LocalDate today = LocalDate.now();
        DailyInvoiceReportDto report = dailyInvoiceService.getDailyInvoices(today);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/range")
    public ResponseEntity<List<DailyInvoiceReportDto>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyInvoiceReportDto> reports = dailyInvoiceService.getDailyInvoicesForRange(startDate, endDate);
        return ResponseEntity.ok(reports);
    }
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> summary = dailyInvoiceService.getDailySummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }
    
    // Search invoices by customer/vendor name for a specific date
    @GetMapping("/search")
    public ResponseEntity<List<DailyInvoiceDto>> searchInvoices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type) {
        
        DailyInvoiceReportDto report = dailyInvoiceService.getDailyInvoices(date);
        List<DailyInvoiceDto> allInvoices = new ArrayList<>();
        allInvoices.addAll(report.getCustomerInvoices());
        allInvoices.addAll(report.getVendorInvoices());
        
        // Filter by name if provided
        if (name != null && !name.isEmpty()) {
            allInvoices = allInvoices.stream()
                .filter(invoice -> invoice.getCustomerOrVendorName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // Filter by type if provided
        if (type != null && !type.isEmpty()) {
            allInvoices = allInvoices.stream()
                .filter(invoice -> invoice.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(allInvoices);
    }

    @GetMapping("/api/daily-payments/date/{date}")
    public ResponseEntity<DailyPaymentsReportDto> getDailyPayments(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            DailyPaymentsReportDto report = dailyInvoiceService.getDailyPayments(date);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/daily-expenses/date/{date}")
    public ResponseEntity<DailyExpensesReportDto> getDailyExpenses(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            DailyExpensesReportDto report = dailyInvoiceService.getDailyExpenses(date);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}