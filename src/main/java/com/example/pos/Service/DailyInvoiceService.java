package com.example.pos.Service;

import com.example.pos.DTO.*;
import com.example.pos.entity.pos.*;
import com.example.pos.repo.pos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DailyInvoiceService {
    
    @Autowired
    private CustomerInvoiceRecordRepo customerInvoiceRepo;
    
    @Autowired
    private CompanyInvoiceAmountRepo companyInvoiceRepo;
    
    @Autowired
    private SalesRepo salesRepo;
    
    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CustomerPaymentTimeRepo customerPaymentTimeRepo;

    @Autowired
    private CompanyPaymentTimeRepo companyPaymentTimeRepo;

    @Autowired
    private ExpenseRepository expenseRepository;

    public DailyInvoiceReportDto getDailyInvoices(LocalDate date) {
        DailyInvoiceReportDto report = new DailyInvoiceReportDto();
        report.setDate(date);

        // 1. Get customer invoices for the date
        List<CustomerInvoiceRecord> customerInvoices = customerInvoiceRepo.findCustomerInvoicesByDate(date);
        List<DailyInvoiceDto> customerInvoiceDtos = new ArrayList<>();
        BigDecimal totalCustomerSales = BigDecimal.ZERO;

        for (CustomerInvoiceRecord invoice : customerInvoices) {
            DailyInvoiceDto dto = mapCustomerInvoiceToDto(invoice);
            customerInvoiceDtos.add(dto);
            totalCustomerSales = totalCustomerSales.add(invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO);
        }

        // 2. Get vendor invoices for the date
        List<CompanyInvoiceAmount> vendorInvoices = companyInvoiceRepo.findVendorInvoicesByDate(date);
        List<DailyInvoiceDto> vendorInvoiceDtos = new ArrayList<>();
        BigDecimal totalVendorPurchases = BigDecimal.ZERO;

        for (CompanyInvoiceAmount invoice : vendorInvoices) {
            DailyInvoiceDto dto = mapVendorInvoiceToDto(invoice);
            vendorInvoiceDtos.add(dto);
            totalVendorPurchases = totalVendorPurchases.add(invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO);
        }

        // Set all data to report
        report.setCustomerInvoices(customerInvoiceDtos);
        report.setVendorInvoices(vendorInvoiceDtos);

        // Calculate totals
        report.setTotalCustomerSales(totalCustomerSales);
        report.setTotalVendorPurchases(totalVendorPurchases);
        report.setTotalInvoices(customerInvoices.size() + vendorInvoices.size());

        // Calculate net sales
        BigDecimal netSales = totalCustomerSales.subtract(totalVendorPurchases);
        report.setNetSales(netSales);

        // Calculate total GST
        BigDecimal totalGst = BigDecimal.ZERO;
        totalGst = totalGst.add(customerInvoices.stream()
                .map(inv -> inv.getGstAmount() != null ? inv.getGstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        totalGst = totalGst.add(vendorInvoices.stream()
                .map(inv -> inv.getGstAmount() != null ? inv.getGstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setTotalGstAmount(totalGst);

        return report;
    }

    private DailyPaymentDto mapCustomerPaymentToDto(CustomerPaymentTime payment) {
        DailyPaymentDto dto = new DailyPaymentDto();
        dto.setPayerName(payment.getCustomerName());
        dto.setAmountPaid(payment.getAmountPaid());
        dto.setPaymentTime(payment.getPaymentTime());
        dto.setInvoiceNumber(payment.getInvoiceNumber());
        dto.setBillingMonth(payment.getBillingMonth() != null ?
                payment.getBillingMonth().toString() : null);
        dto.setType("CUSTOMER");
        return dto;
    }

    private DailyPaymentDto mapVendorPaymentToDto(CompanyPaymentTime payment) {
        DailyPaymentDto dto = new DailyPaymentDto();
        dto.setPayerName(payment.getVendorName());
        dto.setAmountPaid(payment.getAmountPaid());
        dto.setPaymentTime(payment.getPaymentTime());
        dto.setInvoiceNumber(payment.getInvoiceNumber());
        dto.setBillingMonth(payment.getBillingMonth() != null ?
                payment.getBillingMonth().toString() : null);
        dto.setType("VENDOR");
        return dto;
    }

    private DailyExpenseDto mapExpenseToDto(Expenses expense) {
        DailyExpenseDto dto = new DailyExpenseDto();
        dto.setCategory(expense.getCategory());
        dto.setDescription(expense.getDescription());
        dto.setAmount(BigDecimal.valueOf(expense.getAmount()));
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setExpenseTime(expense.getExpenseTime());
        dto.setExpenseType(expense.getExpenseType());
        return dto;
    }
    private DailyInvoiceDto mapCustomerInvoiceToDto(CustomerInvoiceRecord invoice) {
        DailyInvoiceDto dto = new DailyInvoiceDto();
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCustomerOrVendorName(invoice.getCustomerName());
        dto.setType("Customer");
        dto.setStatus(invoice.getStatus());
        dto.setGrandTotal(invoice.getGrandTotal());
        dto.setGstAmount(invoice.getGstAmount());
        dto.setTotalBeforeGst(invoice.getTotalBeforeGst());
        dto.setAmountPaid(invoice.getAmountPaid());

        
        // Get invoice time from sales records
        LocalDateTime invoiceTime = salesRepo.findLatestSaleTimeByInvoice(invoice.getInvoiceNumber());
        dto.setInvoiceTime(invoiceTime);
        
        // Get products for this invoice
        List<SalesEntity> sales = salesRepo.findByInvoiceNumberAndIsActive(invoice.getInvoiceNumber(), true);
        List<DailyInvoiceProductDto> products = sales.stream()
            .map(sale -> {
                DailyInvoiceProductDto product = new DailyInvoiceProductDto();
                product.setProductName(sale.getProductName());
                product.setCategory(sale.getCategory());
                product.setQuantity("Return".equalsIgnoreCase(invoice.getStatus()) ? 
                    sale.getReturnedQuantity() : sale.getQuantity());
                product.setTotalPrice(sale.getTotalPrice());
                return product;
            })
            .collect(Collectors.toList());
        dto.setProducts(products);
        
        return dto;
    }
    
    private DailyInvoiceDto mapVendorInvoiceToDto(CompanyInvoiceAmount invoice) {
        DailyInvoiceDto dto = new DailyInvoiceDto();
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCustomerOrVendorName(invoice.getVendorName());
        dto.setType("Vendor");
        dto.setStatus(invoice.getStatus());
        dto.setGrandTotal(invoice.getGrandTotal());
        dto.setGstAmount(invoice.getGstAmount());
        dto.setTotalBeforeGst(invoice.getTotalBeforeGst());
        dto.setAmountPaid(invoice.getAmountPaid());
        
        // Get invoice time from product records
        LocalDateTime invoiceTime = productRepo.findLatestProductTimeByInvoice(invoice.getInvoiceNumber());
        dto.setInvoiceTime(invoiceTime);
        
        // Get products for this invoice
        List<ProductEntity> products = productRepo.findByInvoiceNumberAndIsActive(invoice.getInvoiceNumber(), true);
        List<DailyInvoiceProductDto> productDtos = products.stream()
            .map(product -> {
                DailyInvoiceProductDto prodDto = new DailyInvoiceProductDto();
                prodDto.setProductName(product.getProductName());
                prodDto.setCategory(product.getCategory());
                prodDto.setQuantity("Return".equalsIgnoreCase(invoice.getStatus()) ? 
                    product.getReturnedQuantity() : product.getQuantity());
                prodDto.setTotalPrice(product.getTotalPrice());
                return prodDto;
            })
            .collect(Collectors.toList());
        dto.setProducts(productDtos);
        
        return dto;
    }
    
    // Get invoices for a date range
    public List<DailyInvoiceReportDto> getDailyInvoicesForRange(LocalDate startDate, LocalDate endDate) {
        List<DailyInvoiceReportDto> reports = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DailyInvoiceReportDto dailyReport = getDailyInvoices(currentDate);
            reports.add(dailyReport);
            currentDate = currentDate.plusDays(1);
        }
        
        return reports;
    }
    
    // Get summary for a date range
    public Map<String, Object> getDailySummary(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new HashMap<>();
        
        List<DailyInvoiceReportDto> reports = getDailyInvoicesForRange(startDate, endDate);
        
        BigDecimal totalCustomerSales = reports.stream()
            .map(DailyInvoiceReportDto::getTotalCustomerSales)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalVendorPurchases = reports.stream()
            .map(DailyInvoiceReportDto::getTotalVendorPurchases)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalGst = reports.stream()
            .map(DailyInvoiceReportDto::getTotalGstAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalInvoices = reports.stream()
            .mapToInt(DailyInvoiceReportDto::getTotalInvoices)
            .sum();
        
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);
        summary.put("totalCustomerSales", totalCustomerSales);
        summary.put("totalVendorPurchases", totalVendorPurchases);
        summary.put("totalGstAmount", totalGst);
        summary.put("totalInvoices", totalInvoices);
        summary.put("netBalance", totalCustomerSales.subtract(totalVendorPurchases));
        summary.put("dailyReports", reports);
        
        return summary;
    }

    public DailyPaymentsReportDto getDailyPayments(LocalDate date) {
        DailyPaymentsReportDto report = new DailyPaymentsReportDto();
        report.setDate(date);

        // 1. Get customer payments for the date
        List<CustomerPaymentTime> customerPayments = customerPaymentTimeRepo.findByPaymentDate(date);
        List<DailyPaymentDto> customerPaymentDtos = new ArrayList<>();
        BigDecimal totalCustomerPayments = BigDecimal.ZERO;

        for (CustomerPaymentTime payment : customerPayments) {
            DailyPaymentDto dto = mapCustomerPaymentToDto(payment);
            customerPaymentDtos.add(dto);
            totalCustomerPayments = totalCustomerPayments.add(payment.getAmountPaid() != null ? payment.getAmountPaid() : BigDecimal.ZERO);
        }

        // 2. Get vendor payments for the date
        List<CompanyPaymentTime> vendorPayments = companyPaymentTimeRepo.findByPaymentDate(date);
        List<DailyPaymentDto> vendorPaymentDtos = new ArrayList<>();
        BigDecimal totalVendorPayments = BigDecimal.ZERO;

        for (CompanyPaymentTime payment : vendorPayments) {
            DailyPaymentDto dto = mapVendorPaymentToDto(payment);
            vendorPaymentDtos.add(dto);
            totalVendorPayments = totalVendorPayments.add(payment.getAmountPaid() != null ? payment.getAmountPaid() : BigDecimal.ZERO);
        }

        // Set all data to report
        report.setCustomerPayments(customerPaymentDtos);
        report.setVendorPayments(vendorPaymentDtos);

        // Calculate totals
        report.setTotalCustomerPayments(totalCustomerPayments);
        report.setTotalVendorPayments(totalVendorPayments);
        report.setTotalPayments(customerPayments.size() + vendorPayments.size());

        // Calculate net payment flow
        BigDecimal netPaymentFlow = totalCustomerPayments.subtract(totalVendorPayments);
        report.setNetPaymentFlow(netPaymentFlow);

        return report;
    }

    public DailyExpensesReportDto getDailyExpenses(LocalDate date) {
        DailyExpensesReportDto report = new DailyExpensesReportDto();
        report.setDate(date);

        // Get daily expenses for the date
        List<Expenses> dailyExpenses = expenseRepository.findDailyExpensesByDate(date);
        List<DailyExpenseDto> expenseDtos = new ArrayList<>();
        BigDecimal totalDailyExpenses = BigDecimal.ZERO;

        for (Expenses expense : dailyExpenses) {
            DailyExpenseDto dto = mapExpenseToDto(expense);
            expenseDtos.add(dto);
            totalDailyExpenses = totalDailyExpenses.add(BigDecimal.valueOf(expense.getAmount()));
        }

        // Set all data to report
        report.setDailyExpenses(expenseDtos);
        report.setTotalDailyExpenses(totalDailyExpenses);
        report.setTotalExpenses(dailyExpenses.size());

        // You can add more calculations if needed
        // e.g., expenses by category
        Map<String, BigDecimal> expensesByCategory = dailyExpenses.stream()
                .collect(Collectors.groupingBy(
                        Expenses::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                exp -> BigDecimal.valueOf(exp.getAmount()),
                                BigDecimal::add
                        )
                ));
        report.setExpensesByCategory(expensesByCategory);

        return report;
    }
}