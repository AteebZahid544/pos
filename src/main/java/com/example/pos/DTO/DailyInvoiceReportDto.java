package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyInvoiceReportDto {
    private LocalDate date;
    private List<DailyInvoiceDto> customerInvoices;
    private List<DailyInvoiceDto> vendorInvoices;
    private BigDecimal totalCustomerSales;
    private BigDecimal totalVendorPurchases;
    private BigDecimal totalGstAmount;
    private int totalInvoices;

    private BigDecimal totalCustomerPayments;
    private BigDecimal totalVendorPayments;
    private BigDecimal totalDailyExpenses;
    private BigDecimal netCashFlow; // (Customer Payments - Vendor Payments - Daily Expenses)

    private List<DailyPaymentDto> customerPayments;
    private List<DailyPaymentDto> vendorPayments;
    private List<DailyExpenseDto> dailyExpenses;
    // Summary statistics
    private DailySummaryDto summary;
    private BigDecimal netSales;

}