package com.example.pos.Service;

import com.example.pos.DTO.LedgerInvoiceDto;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerLedgerService {

    private final CustomerInvoiceRecordRepo invoiceRepo;
    private final CompanyInvoiceAmountRepo companyInvoiceAmountRepo;

    private final SalesRepo salesRepo;
    private final ProductRepo productRepo;

    public LedgerResponseDto getCustomerLedger(String customerName) {

        // Fetch all active invoices (Sale and Return)
        List<CustomerInvoiceRecord> invoices =
                invoiceRepo.findByCustomerNameAndIsActiveTrueAndStatusInOrderByIdAsc(
                        customerName, List.of("Sale", "Return")
                );

        LedgerResponseDto response = new LedgerResponseDto();
        response.setCustomerName(customerName);

        List<LedgerInvoiceDto> invoiceDtos = new ArrayList<>();
        BigDecimal cumulativeBalance = BigDecimal.ZERO;

        for (CustomerInvoiceRecord invoice : invoices) {
            LedgerInvoiceDto invoiceDto = new LedgerInvoiceDto();
            invoiceDto.setInvoiceNumber(invoice.getInvoiceNumber());
            invoiceDto.setGrandTotal(invoice.getGrandTotal());
            invoiceDto.setAmountPaid(invoice.getAmountPaid());
            invoiceDto.setStatus(invoice.getStatus());

            // Balance before
            BigDecimal balanceBefore = cumulativeBalance;

            // Balance after
            BigDecimal balanceAfter;
            if ("RETURN".equalsIgnoreCase(invoice.getStatus())) {
                balanceAfter = balanceBefore.subtract(invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO);
            } else {
                balanceAfter = balanceBefore.add(invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO);
            }

            cumulativeBalance = balanceAfter;

            invoiceDto.setBalanceBefore(balanceBefore);
            invoiceDto.setBalanceAfter(balanceAfter);

            // Fetch products **based on invoice type**
            List<SalesEntity> sales;
            if ("RETURN".equalsIgnoreCase(invoice.getStatus())) {
                // Only products that were returned
                sales = salesRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoice.getInvoiceNumber(), "Return",true);
            } else {
                // Only products that were sold (not returned)
                sales = salesRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoice.getInvoiceNumber(), "Sale",true);
            }
            invoiceDto.setSaleRecordTime(sales.get(0).getSaleEntryTime());
            invoiceDto.setReturnRecordTime(sales.get(0).getReturnTime());

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

        response.setInvoices(invoiceDtos);
        return response;
    }

    public LedgerResponseDto getVendorLedger(String vendorName) {

        // Fetch all active invoices (Sale and Return)
        List<CompanyInvoiceAmount> invoices =
                companyInvoiceAmountRepo.findByVendorNameAndIsActiveTrueAndStatusInOrderByIdAsc(
                        vendorName, List.of("Purchase", "Return")
                );

        LedgerResponseDto response = new LedgerResponseDto();
        response.setVendorName(vendorName);

        List<LedgerInvoiceDto> invoiceDtos = new ArrayList<>();
        BigDecimal cumulativeBalance = BigDecimal.ZERO;

        for (CompanyInvoiceAmount invoice : invoices) {
            LedgerInvoiceDto invoiceDto = new LedgerInvoiceDto();
            invoiceDto.setInvoiceNumber(invoice.getInvoiceNumber());
            invoiceDto.setGrandTotal(invoice.getGrandTotal());
            invoiceDto.setAmountPaid(invoice.getAmountPaid());
            invoiceDto.setStatus(invoice.getStatus());

            // Balance before
            BigDecimal balanceBefore = cumulativeBalance;

            // Balance after
            BigDecimal balanceAfter;
            if ("Purchase".equalsIgnoreCase(invoice.getStatus())) {
                balanceAfter = balanceBefore.add(invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO);
            } else {
                balanceAfter = balanceBefore.subtract(invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO);
            }

            cumulativeBalance = balanceAfter;

            invoiceDto.setBalanceBefore(balanceBefore);
            invoiceDto.setBalanceAfter(balanceAfter);

            // Fetch products **based on invoice type**
            List<ProductEntity> purchase;
            if ("Return".equalsIgnoreCase(invoice.getStatus())) {
                // Only products that were returned
                purchase = productRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoice.getInvoiceNumber(), "Return",true);
            } else {
                // Only products that were sold (not returned)
                purchase = productRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoice.getInvoiceNumber(), "Purchase",true);
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

        response.setInvoices(invoiceDtos);
        return response;
    }
}
