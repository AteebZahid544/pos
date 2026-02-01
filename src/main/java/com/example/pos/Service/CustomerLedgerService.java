package com.example.pos.Service;

import com.example.pos.DTO.LedgerInvoiceDto;
import com.example.pos.DTO.LedgerMonthDto;
import com.example.pos.DTO.LedgerProductDto;
import com.example.pos.DTO.LedgerResponseDto;
import com.example.pos.entity.pos.CompanyInvoiceAmount;
import com.example.pos.entity.pos.CustomerInvoiceRecord;
import com.example.pos.entity.pos.ProductEntity;
import com.example.pos.entity.pos.SalesEntity;
import com.example.pos.repo.pos.CompanyInvoiceAmountRepo;
import com.example.pos.repo.pos.CustomerInvoiceRecordRepo;
import com.example.pos.repo.pos.ProductRepo;
import com.example.pos.repo.pos.SalesRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerLedgerService {

    private final CustomerInvoiceRecordRepo invoiceRepo;
    private final CompanyInvoiceAmountRepo companyInvoiceAmountRepo;

    private final SalesRepo salesRepo;
    private final ProductRepo productRepo;

    public LedgerResponseDto getCustomerLedger(String customerName, YearMonth startMonth, YearMonth endMonth) {

        LedgerResponseDto response = new LedgerResponseDto();
        response.setCustomerName(customerName);
        response.setStartMonth(startMonth);
        response.setEndMonth(endMonth);

        List<CustomerInvoiceRecord> invoices;

        if (startMonth != null && endMonth != null) {
            // Use custom query with month range
            invoices = invoiceRepo.findCustomerLedgerByMonthRange(
                    customerName,
                    List.of("Sale", "Return"),
                    startMonth,
                    endMonth
            );
        } else {
            // Use original query if no months provided
            invoices = invoiceRepo.findByCustomerNameAndIsActiveTrueAndStatusInOrderByIdDesc(
                    customerName,
                    List.of("Sale", "Return")
            );
        }

        // Group invoices by month for better organization
        Map<YearMonth, List<CustomerInvoiceRecord>> invoicesByMonth = invoices.stream()
                .collect(Collectors.groupingBy(CustomerInvoiceRecord::getBillingMonth));

        // Calculate cumulative balance
        BigDecimal cumulativeBalance = BigDecimal.ZERO;
        List<LedgerMonthDto> monthLedgers = new ArrayList<>();

        // Process each month separately
        for (Map.Entry<YearMonth, List<CustomerInvoiceRecord>> entry : invoicesByMonth.entrySet()) {
            YearMonth month = entry.getKey();
            List<CustomerInvoiceRecord> monthInvoices = entry.getValue();

            LedgerMonthDto monthLedger = new LedgerMonthDto();
            monthLedger.setBillingMonth(month);
            monthLedger.setMonthStartBalance(cumulativeBalance); // Balance at start of month

            List<LedgerInvoiceDto> invoiceDtos = new ArrayList<>();
            BigDecimal monthCumulativeBalance = cumulativeBalance;

            // Process invoices for this month
            for (CustomerInvoiceRecord invoice : monthInvoices) {
                LedgerInvoiceDto invoiceDto = new LedgerInvoiceDto();
                invoiceDto.setInvoiceNumber(invoice.getInvoiceNumber());
                invoiceDto.setGrandTotal(invoice.getGrandTotal());
                invoiceDto.setAmountPaid(invoice.getAmountPaid());
                invoiceDto.setStatus(invoice.getStatus());
                invoiceDto.setBillingMonth(invoice.getBillingMonth());
                invoiceDto.setGstPercentage(invoice.getGstPercentage());
                invoiceDto.setGstAmount(invoice.getGstAmount());
                invoiceDto.setTotalBeforeGst(invoice.getTotalBeforeGst());

                // Balance before this invoice
                BigDecimal balanceBefore = monthCumulativeBalance;

                // Balance after this invoice
                BigDecimal balanceAfter;
                if ("RETURN".equalsIgnoreCase(invoice.getStatus())) {
                    balanceAfter = balanceBefore.subtract(
                            invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO
                    );
                } else {
                    balanceAfter = balanceBefore.add(
                            invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO
                    );
                }

                monthCumulativeBalance = balanceAfter;
                cumulativeBalance = balanceAfter;

                invoiceDto.setBalanceBefore(balanceBefore);
                invoiceDto.setBalanceAfter(balanceAfter);

                // Fetch products based on invoice type
                List<SalesEntity> sales;
                if ("RETURN".equalsIgnoreCase(invoice.getStatus())) {
                    sales = salesRepo.findAllByInvoiceNumberAndStatusAndIsActive(
                            invoice.getInvoiceNumber(), "Return", true
                    );
                } else {
                    sales = salesRepo.findAllByInvoiceNumberAndStatusAndIsActive(
                            invoice.getInvoiceNumber(), "Sale", true
                    );
                }

                if (!sales.isEmpty()) {
                    invoiceDto.setSaleRecordTime(sales.get(0).getSaleEntryTime());
                    invoiceDto.setReturnRecordTime(sales.get(0).getReturnTime());
                }

                List<LedgerProductDto> products = sales.stream().map(sale -> {
                    LedgerProductDto p = new LedgerProductDto();
                    p.setProductName(sale.getProductName());
                    p.setCategory(sale.getCategory());

                    if ("RETURN".equalsIgnoreCase(invoice.getStatus())) {
                        p.setQuantity(sale.getReturnedQuantity());
                    } else {
                        p.setQuantity(sale.getQuantity());
                    }

                    p.setTotalPrice(sale.getTotalPrice());
                    return p;
                }).collect(Collectors.toList());

                invoiceDto.setProducts(products);
                invoiceDtos.add(invoiceDto);
            }

            monthLedger.setInvoices(invoiceDtos);
            monthLedger.setMonthEndBalance(monthCumulativeBalance); // Balance at end of month

            // Calculate month summary
            BigDecimal monthTotalSales = monthInvoices.stream()
                    .filter(inv -> "Sale".equalsIgnoreCase(inv.getStatus()))
                    .map(inv -> inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthTotalReturns = monthInvoices.stream()
                    .filter(inv -> "Return".equalsIgnoreCase(inv.getStatus()))
                    .map(inv -> inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthLedger.setMonthTotalSales(monthTotalSales);
            monthLedger.setMonthTotalReturns(monthTotalReturns);
            monthLedger.setMonthNetChange(monthTotalSales.subtract(monthTotalReturns));

            monthLedgers.add(monthLedger);
        }

        // Sort months chronologically
        monthLedgers.sort(Comparator.comparing(LedgerMonthDto::getBillingMonth));

        response.setMonthLedgers(monthLedgers);
        response.setTotalBalance(cumulativeBalance);

        return response;
    }

    // Alternative: For single month
    public LedgerResponseDto getCustomerLedgerForMonth(String customerName, YearMonth billingMonth) {
        LedgerResponseDto response = new LedgerResponseDto();
        response.setCustomerName(customerName);
        response.setBillingMonth(billingMonth);

        List<CustomerInvoiceRecord> invoices = invoiceRepo.findCustomerLedgerByMonth(
                customerName,
                List.of("Sale", "Return"),
                billingMonth
        );

        // ... rest of processing for single month ...

        return response;
    }

    public LedgerResponseDto getVendorLedger(String vendorName, YearMonth startMonth, YearMonth endMonth) {

        LedgerResponseDto response = new LedgerResponseDto();
        response.setVendorName(vendorName);
        response.setStartMonth(startMonth);
        response.setEndMonth(endMonth);

        List<CompanyInvoiceAmount> invoices;

        if (startMonth != null && endMonth != null) {
            // Use custom query with month range
            invoices = companyInvoiceAmountRepo.findVendorLedgerByMonthRange(
                    vendorName,
                    List.of("Purchase", "Return"),
                    startMonth,
                    endMonth
            );
        } else {
            // Use original query if no months provided
            invoices = companyInvoiceAmountRepo.findByVendorNameAndIsActiveTrueAndStatusInOrderByIdDesc(
                    vendorName,
                    List.of("Purchase", "Return")
            );
        }

        // Group invoices by month for better organization
        Map<YearMonth, List<CompanyInvoiceAmount>> invoicesByMonth = invoices.stream()
                .filter(invoice -> invoice.getBillingMonth() != null)
                .collect(Collectors.groupingBy(CompanyInvoiceAmount::getBillingMonth));

        // Calculate cumulative balance
        BigDecimal cumulativeBalance = BigDecimal.ZERO;
        List<LedgerMonthDto> monthLedgers = new ArrayList<>();

        // Process each month separately
        for (Map.Entry<YearMonth, List<CompanyInvoiceAmount>> entry : invoicesByMonth.entrySet()) {
            YearMonth month = entry.getKey();
            List<CompanyInvoiceAmount> monthInvoices = entry.getValue();

            LedgerMonthDto monthLedger = new LedgerMonthDto();
            monthLedger.setBillingMonth(month);
            monthLedger.setMonthStartBalance(cumulativeBalance); // Balance at start of month

            List<LedgerInvoiceDto> invoiceDtos = new ArrayList<>();
            BigDecimal monthCumulativeBalance = cumulativeBalance;

            // Calculate month totals
            BigDecimal monthTotalPurchases = BigDecimal.ZERO;
            BigDecimal monthTotalReturns = BigDecimal.ZERO;

            // Process invoices for this month
            for (CompanyInvoiceAmount invoice : monthInvoices) {
                LedgerInvoiceDto invoiceDto = new LedgerInvoiceDto();
                invoiceDto.setInvoiceNumber(invoice.getInvoiceNumber());
                invoiceDto.setGrandTotal(invoice.getGrandTotal());
                invoiceDto.setAmountPaid(invoice.getAmountPaid());
                invoiceDto.setStatus(invoice.getStatus());
                invoiceDto.setBillingMonth(invoice.getBillingMonth());

                // Set GST fields if they exist in your entity
                if (invoice.getGstPercentage() != null) {
                    invoiceDto.setGstPercentage(invoice.getGstPercentage());
                    invoiceDto.setGstAmount(invoice.getGstAmount());
                    invoiceDto.setTotalBeforeGst(invoice.getTotalBeforeGst());
                }

                // Balance before this invoice
                BigDecimal balanceBefore = monthCumulativeBalance;

                // Balance after this invoice
                BigDecimal balanceAfter;
                if ("Purchase".equalsIgnoreCase(invoice.getStatus())) {
                    balanceAfter = balanceBefore.add(
                            invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO
                    );
                    monthTotalPurchases = monthTotalPurchases.add(
                            invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO
                    );
                } else {
                    balanceAfter = balanceBefore.subtract(
                            invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO
                    );
                    monthTotalReturns = monthTotalReturns.add(
                            invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO
                    );
                }

                monthCumulativeBalance = balanceAfter;
                cumulativeBalance = balanceAfter;

                invoiceDto.setBalanceBefore(balanceBefore);
                invoiceDto.setBalanceAfter(balanceAfter);

                // Fetch products based on invoice type
                List<ProductEntity> purchase;
                if ("Return".equalsIgnoreCase(invoice.getStatus())) {
                    purchase = productRepo.findAllByInvoiceNumberAndStatusAndIsActive(
                            invoice.getInvoiceNumber(), "Return", true
                    );
                } else {
                    purchase = productRepo.findAllByInvoiceNumberAndStatusAndIsActive(
                            invoice.getInvoiceNumber(), "Purchase", true
                    );
                }

                if (!purchase.isEmpty()) {
                    invoiceDto.setPurchaseRecordTime(purchase.get(0).getProductEntryTime());
                    invoiceDto.setReturnRecordTime(purchase.get(0).getReturnTime());
                } else {
                    invoiceDto.setPurchaseRecordTime(null);
                    invoiceDto.setReturnRecordTime(null);
                }

                List<LedgerProductDto> products = purchase.stream().map(purchases -> {
                    LedgerProductDto p = new LedgerProductDto();
                    p.setProductName(purchases.getProductName());
                    p.setCategory(purchases.getCategory());

                    if ("Return".equalsIgnoreCase(invoice.getStatus())) {
                        p.setQuantity(purchases.getReturnedQuantity());
                    } else {
                        p.setQuantity(purchases.getQuantity());
                    }

                    p.setTotalPrice(purchases.getTotalPrice());
                    return p;
                }).collect(Collectors.toList());

                invoiceDto.setProducts(products);
                invoiceDtos.add(invoiceDto);
            }

            monthLedger.setInvoices(invoiceDtos);
            monthLedger.setMonthEndBalance(monthCumulativeBalance); // Balance at end of month
            monthLedger.setMonthTotalPurchases(monthTotalPurchases);
            monthLedger.setMonthTotalReturns(monthTotalReturns);
            monthLedger.setMonthNetChange(monthTotalPurchases.subtract(monthTotalReturns));

            monthLedgers.add(monthLedger);
        }

        // Sort months chronologically
        monthLedgers.sort(Comparator.comparing(LedgerMonthDto::getBillingMonth));

        response.setMonthLedgers(monthLedgers);
        response.setTotalBalance(cumulativeBalance);

        return response;
    }

    // For single month
    public LedgerResponseDto getVendorLedgerForMonth(String vendorName, YearMonth billingMonth) {
        LedgerResponseDto response = new LedgerResponseDto();
        response.setVendorName(vendorName);
        response.setBillingMonth(billingMonth);

        List<CompanyInvoiceAmount> invoices = companyInvoiceAmountRepo.findVendorLedgerByMonth(
                vendorName,
                List.of("Purchase", "Return"),
                billingMonth
        );

        // Process invoices similar to above, but for single month
        // ... (similar processing logic as above for single month)

        return response;
    }
}
