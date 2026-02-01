package com.example.pos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class MonthlySummaryDto {
    private BigDecimal inventoryValue;
    private BigDecimal companyBalance;
    private BigDecimal customerBalance;
    private BigDecimal expenses;
    private BigDecimal companyPaidAmount;
    private BigDecimal customerPaidAmount;
    private BigDecimal salary;
    private BigDecimal product;
    private BigDecimal sale;
    private BigDecimal grossProfit;
    private BigDecimal netProfit;
    private List<GraphSalesDto> topProducts;
    private List<CustomerPurchaseDto> topCustomers;

    private BigDecimal lastMonthSale;
    private String saleComparison; // UP / DOWN / SAME
    private List<MonthlySalesGraphDto> monthlySalesGraph;


}
