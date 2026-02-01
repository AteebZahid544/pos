package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyPaymentsReportDto {
    private LocalDate date;
    private List<DailyPaymentDto> customerPayments;
    private List<DailyPaymentDto> vendorPayments;
    private BigDecimal totalCustomerPayments;
    private BigDecimal totalVendorPayments;
    private Integer totalPayments;
    private BigDecimal netPaymentFlow;
}