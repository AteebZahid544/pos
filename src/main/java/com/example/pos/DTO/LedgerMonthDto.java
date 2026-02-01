package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
public class LedgerMonthDto {
    private YearMonth billingMonth;
    private BigDecimal monthStartBalance;
    private BigDecimal monthEndBalance;
    private BigDecimal monthTotalSales;
    private BigDecimal monthTotalReturns;
    private BigDecimal monthNetChange;
    private List<LedgerInvoiceDto> invoices;
    private BigDecimal monthTotalPurchases; // Changed from monthTotalSales


}