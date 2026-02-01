package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailySummaryDto {
    private BigDecimal totalRevenue; // Customer Sales
    private BigDecimal totalPurchases; // Vendor Purchases
    private BigDecimal totalPaymentsReceived; // Customer Payments
    private BigDecimal totalPaymentsMade; // Vendor Payments
    private BigDecimal totalExpenses;
    private BigDecimal netProfit; // Revenue - Purchases - Expenses
    private BigDecimal cashOnHand; // Starting from previous day's balance
}