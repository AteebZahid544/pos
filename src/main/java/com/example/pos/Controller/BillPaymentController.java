package com.example.pos.Controller;

import com.example.pos.DTO.CompanyPayBillDto;
import com.example.pos.DTO.CustomerPayBillDto;
import com.example.pos.Service.BillPaymentService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billPayment")
public class BillPaymentController {

    @Autowired
    private BillPaymentService billPaymentService;

    @PostMapping("/company_pay_bill")
    public Status companyPayBill(@RequestBody CompanyPayBillDto companyPayBillDto) {
        return billPaymentService.payVendorBill(companyPayBillDto.getVendorName(), companyPayBillDto.getAmount());
    }

    @PostMapping("/customer_pay_bill")
    public Status customerPayBill(@RequestBody CustomerPayBillDto customerPayBillDto){
        return billPaymentService.customerPayBill(customerPayBillDto.getCustomerName(),customerPayBillDto.getAmount());
    }

//    @DeleteMapping("/delete/{id}")
//    public Status deleteEntry(@PathVariable int id) {
//        return billPaymentService.deleteEntry(id);
//    }
}
