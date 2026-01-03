package com.example.pos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class LedgerResponseDto {

    private String customerName;
    private String vendorName;

    private List<LedgerInvoiceDto> invoices;
}
