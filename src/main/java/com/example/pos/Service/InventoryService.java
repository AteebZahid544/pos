package com.example.pos.Service;

import com.example.pos.DTO.*;
import com.example.pos.entity.pos.CompanyPaymentTime;
import com.example.pos.entity.pos.CustomerPaymentTime;
import com.example.pos.entity.pos.InventoryEntity;

import com.example.pos.repo.pos.*;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;


@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepo inventoryRepo;
    private final CustomerPaymentTimeRepo customerPaymentTimeRepo;
    private final CompanyPaymentTimeRepo companyPaymentTime;
    private final CompanyBillAmountPaidRepo companyBillAmountPaidRepo;
    private final CustomerBillAmountPaidRepo customerBillAmountPaidRepo;
    private final ExpenseRepository expenseRepository;
    private final CompanyPaymentTimeRepo companyPaymentTimeRepo;
    private final EmployeeSalaryRepository employeeSalaryRepository;
    private final CustomerInvoiceRecordRepo customerInvoiceRecordRepo;
    private final CompanyInvoiceAmountRepo companyInvoiceAmountRepo;
    private final SalesRepo salesRepo;


    public Status getInventoryGroupedByCategory(String category, YearMonth month) {

        // If month is provided, fetch records for that specific month
        List<InventoryEntity> inventoryList;

        if (month != null) {
            // Fetch records for specific month
            if (category != null && !category.trim().isEmpty()) {
                // Both category and month provided
                inventoryList = inventoryRepo.findByCategoryAndAddedMonthOrderByAddedMonthDesc(
                        category.trim(), month);
            } else {
                // Only month provided, fetch all records for that month
                inventoryList = inventoryRepo.findByAddedMonthOrderByAddedMonthDesc(month);
            }
        } else {
            // No month filter, get latest records for each category
            inventoryList = inventoryRepo.findLatestRecordForEachCategory();
        }

        if (inventoryList == null || inventoryList.isEmpty()) {
            String message = month != null ?
                    "No inventory found for " + month :
                    "No inventory found";
            return new Status(StatusMessage.FAILURE, message);
        }

        // If category is provided → filter first (only needed when month is null or both filters)
        if (category != null && !category.trim().isEmpty() && month == null) {
            String requestedCategory = category.trim().toLowerCase();

            List<InventoryEntity> filteredList = inventoryList.stream()
                    .filter(inv -> inv.getCategory() != null &&
                            inv.getCategory().equalsIgnoreCase(requestedCategory))
                    .collect(Collectors.toList());

            if (filteredList.isEmpty()) {
                return new Status(
                        StatusMessage.FAILURE,
                        "No inventory found for category: " + category
                );
            }

            Map<String, List<InventoryEntity>> singleCategoryMap = new HashMap<>();
            singleCategoryMap.put(requestedCategory, filteredList);

            return new Status(StatusMessage.SUCCESS, singleCategoryMap);
        }

        // No specific category or both filters applied → return all grouped data
        Map<String, List<InventoryEntity>> groupedInventory = inventoryList.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getCategory().toLowerCase()
                ));

        return new Status(StatusMessage.SUCCESS, groupedInventory);
    }

    public Status getAllPaymentsGroupedByCustomer(LocalDate startDate, LocalDate endDate) {

        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        List<CustomerPaymentTime> payments = customerPaymentTimeRepo.findActivePaymentsWithDateRange(
                startDateTime, endDateTime);

        if (payments == null || payments.isEmpty()) {
            String message = "No active payment records with amount paid > 0 found";
            if (startDate != null || endDate != null) {
                message += " for the selected date range";
            }
            return new Status(
                    StatusMessage.FAILURE,
                    message
            );
        }

        Map<String, List<CustomerPaymentTime>> groupedByCustomer =
                payments.stream()
                        .collect(Collectors.groupingBy(
                                CustomerPaymentTime::getCustomerName
                        ));

        return new Status(
                StatusMessage.SUCCESS,
                groupedByCustomer
        );
    }
    public Status getAllPaymentsGroupedByVendor(LocalDate startDate, LocalDate endDate) {

        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        List<CompanyPaymentTime> payments = companyPaymentTime.findActiveVendorPaymentsWithDateRange(
                startDateTime, endDateTime);

        if (payments == null || payments.isEmpty()) {
            String message = "No active payment records with amount paid > 0 found";
            if (startDate != null || endDate != null) {
                message += " for the selected date range";
            }
            return new Status(
                    StatusMessage.FAILURE,
                    message
            );
        }

        Map<String, List<CompanyPaymentTime>> groupedByVendors =
                payments.stream()
                        .collect(Collectors.groupingBy(
                                CompanyPaymentTime::getVendorName
                        ));

        return new Status(
                StatusMessage.SUCCESS,
                groupedByVendors
        );
    }

//    public BigDecimal getInventoryValueWithFallback() {
//        YearMonth currentMonth = YearMonth.now();
//        LocalDate currentStart = currentMonth.atDay(1);
//        LocalDate currentEnd = currentMonth.atEndOfMonth();
//
//        List<Object[]> currentMonthData = inventoryRepo.findProductsTotalPriceByMonth(currentStart, currentEnd);
//
//        Map<String, BigDecimal> totalMap = new HashMap<>();
//
//        // Add current month products
//        for (Object[] row : currentMonthData) {
//            String productName = (String) row[0];
//            BigDecimal totalPrice = (BigDecimal) row[1];
//            totalMap.put(productName, totalPrice);
//        }
//
//        // Now check last month for missing products
//        YearMonth lastMonth = currentMonth.minusMonths(1);
//        LocalDate lastStart = lastMonth.atDay(1);
//        LocalDate lastEnd = lastMonth.atEndOfMonth();
//
//        List<Object[]> lastMonthData = inventoryRepo.findProductsTotalPriceByMonth(lastStart, lastEnd);
//
//        for (Object[] row : lastMonthData) {
//            String productName = (String) row[0];
//            BigDecimal totalPrice = (BigDecimal) row[1];
//            // Only add if not present in current month
//            totalMap.putIfAbsent(productName, totalPrice);
//        }
//
//        // Sum all
//        BigDecimal sum = totalMap.values().stream()
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return sum;
//    }
//



    public MonthlySummaryDto getMonthlySummary(int year, int month) {

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal inventoryValue =
                inventoryRepo.getInventorySnapshotValue(ym);

        BigDecimal companyBalance =
                companyBillAmountPaidRepo.getCompanyBalanceSnapshot(ym);

        BigDecimal customerBalance =
                customerBillAmountPaidRepo.getCustomerBalanceSnapshot(ym);

        Double expenses =
                expenseRepository.getTotalExpensesBetweenDates(startDate, endDate);

        BigDecimal paidAmount =
                companyPaymentTimeRepo.getTotalAmountPaidByMonth(ym);

        BigDecimal customerPaidAmount =
                customerPaymentTimeRepo.getTotalAmountPaidByMonth(ym);

        Double salary =
                employeeSalaryRepository.getActualSalaryCostByMonth(year, month);

        BigDecimal product =
                companyInvoiceAmountRepo.getNetPurchaseAmountByMonth(year, month);

        // 🔥 CURRENT MONTH SALE (your exact query)
        BigDecimal sale =
                customerInvoiceRecordRepo.getNetSaleAmountByMonth(year, month);

        BigDecimal totalSalesRevenue = customerInvoiceRecordRepo.getNetPurchaseCostByMonth(year, month);

        BigDecimal grossProfit =
                (sale != null ? sale : BigDecimal.ZERO)
                        .subtract(totalSalesRevenue != null ? totalSalesRevenue : BigDecimal.ZERO);

        BigDecimal netProfit =
                grossProfit
                        .subtract(BigDecimal.valueOf(expenses != null ? expenses : 0.0))
                        .subtract(BigDecimal.valueOf(salary != null ? salary : 0.0));

        // =========================
        // 🔥 MONTHLY SALES GRAPH
        // =========================

        List<MonthlySalesGraphDto> monthlySalesGraph = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            BigDecimal monthlySale =
                    customerInvoiceRecordRepo.getNetSaleAmountByMonth(year, m);

            monthlySalesGraph.add(
                    new MonthlySalesGraphDto(
                            Month.of(m).name(),   // JAN, FEB
                            monthlySale != null ? monthlySale : BigDecimal.ZERO
                    )
            );
        }

        // =========================
        // 🔥 CURRENT vs LAST MONTH
        // =========================

        BigDecimal currentMonthSale =
                sale != null ? sale : BigDecimal.ZERO;

        BigDecimal lastMonthSale =
                month > 1
                        ? customerInvoiceRecordRepo.getNetSaleAmountByMonth(year, month - 1)
                        : BigDecimal.ZERO;

        String saleComparison;
        if (currentMonthSale.compareTo(lastMonthSale) > 0) {
            saleComparison = "UP";
        } else if (currentMonthSale.compareTo(lastMonthSale) < 0) {
            saleComparison = "DOWN";
        } else {
            saleComparison = "SAME";
        }

        // =========================
        // 🔹 TOP PRODUCTS & CUSTOMERS (UNCHANGED)
        // =========================
        Pageable topTen = PageRequest.of(0, 10);

        List<GraphSalesDto> topProducts =
                salesRepo.getTopProductsByDateRange(startDate, endDate, topTen);


//        List<GraphSalesDto> topProducts =
//                salesRepo.getTopProducts(year, month);

        List<Object[]> rows =
                customerInvoiceRecordRepo.getTopCustomersRaw(ym.toString());

        List<CustomerPurchaseDto> topCustomers = rows.stream()
                .map(r -> new CustomerPurchaseDto(
                        (String) r[0],
                        (BigDecimal) r[1]
                ))
                .toList();

        // =========================
        // ✅ FINAL RESPONSE
        // =========================

        return new MonthlySummaryDto(
                inventoryValue != null ? inventoryValue : BigDecimal.ZERO,
                companyBalance != null ? companyBalance : BigDecimal.ZERO,
                customerBalance != null ? customerBalance : BigDecimal.ZERO,
                BigDecimal.valueOf(expenses != null ? expenses : 0.0),
                paidAmount != null ? paidAmount : BigDecimal.ZERO,
                customerPaidAmount != null ? customerPaidAmount : BigDecimal.ZERO,
                BigDecimal.valueOf(salary != null ? salary : 0.0),
                product != null ? product : BigDecimal.ZERO,
                currentMonthSale,
                grossProfit,
                netProfit,
                topProducts,
                topCustomers,
                lastMonthSale,
                saleComparison,
                monthlySalesGraph
        );
    }


}
