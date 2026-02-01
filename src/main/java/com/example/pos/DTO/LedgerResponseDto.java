package com.example.pos.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
public class LedgerResponseDto {

    private String customerName;
    private String vendorName;
    private YearMonth startMonth;
    private YearMonth endMonth;
    private YearMonth billingMonth;
    private List<LedgerMonthDto> monthLedgers;
    private BigDecimal totalBalance;


}
