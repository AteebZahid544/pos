package com.example.pos.Controller;

import com.example.pos.DTO.CustomerPayBillDto;
import com.example.pos.DTO.PayBillDto;
import com.example.pos.Service.BillPaymentService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/billPayment")
public class BillPaymentController {

    @Autowired
    private BillPaymentService billPaymentService;

    @PostMapping("/company_pay_bill")
    public Status companyPayBill(@RequestBody PayBillDto companyPayBillDto) {
        return billPaymentService.payVendorBill(companyPayBillDto.getVendorName(), companyPayBillDto.getAmountPaid());
    }

    @GetMapping("/vendor-balance/{vendorName}")
    public Status vendorBalance(@PathVariable String vendorName){
        return billPaymentService.getVendorBalance(vendorName);
    }

    @GetMapping("/customer-balance/{customerName}")
    public Status customerBalance(@PathVariable String customerName){
        return billPaymentService.getCustomerBalance(customerName);
    }

    @PostMapping("/customer_pay_bill")
    public Status customerPayBill(@RequestBody CustomerPayBillDto customerPayBillDto){
        return billPaymentService.customerPayBill(customerPayBillDto.getCustomerName(),customerPayBillDto.getAmountPaid());
    }

    @PutMapping("/update-companyPayBill")
    public Status updatePayBill(@RequestBody PayBillDto companyBill) {
        return billPaymentService.updatePayBill(companyBill.getInvoiceNumber(),companyBill.getStatus(), companyBill.getNewAmountPaid(),companyBill.getVendorName(),companyBill.getPaymentTime());
    }

    @DeleteMapping("/delete-companyPayBill")
    public Status deleteCompanyPayBill(@RequestBody PayBillDto payBillDto){
        return billPaymentService.deleteInvoice(payBillDto.getInvoiceNumber(),payBillDto.getStatus(),payBillDto.getPaymentTime(),payBillDto.getVendorName());
    }

    @PutMapping("/update-customerPayBill")
    public Status updateCustomerPayBill(@RequestBody CustomerPayBillDto customerPayBillDto) {
        return billPaymentService.updateCustomerPayBill(customerPayBillDto.getInvoiceNumber(),customerPayBillDto.getStatus(), customerPayBillDto.getNewAmountPaid(),customerPayBillDto.getCustomerName(),customerPayBillDto.getPaymentTime());
    }

    @DeleteMapping("/delete-customerPayBill")
    public Status deleteCustomerPayBill(@RequestBody CustomerPayBillDto customerPayBillDto){
        return billPaymentService.deleteCustomerInvoice(customerPayBillDto.getInvoiceNumber(),customerPayBillDto.getStatus(),customerPayBillDto.getPaymentTime(),customerPayBillDto.getCustomerName());
    }
}
