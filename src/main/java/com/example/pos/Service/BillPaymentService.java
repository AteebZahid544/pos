package com.example.pos.Service;

import com.example.pos.entity.pos.CompanyBillAmountPaid;
import com.example.pos.entity.pos.CompanyPaymentTime;
import com.example.pos.entity.pos.CustomersBill;
import com.example.pos.repo.pos.CompanyBillAmountPaidRepo;
import com.example.pos.repo.pos.CompanyPaymentTimeRepo;
import com.example.pos.repo.pos.CustomerBillRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BillPaymentService {

    @Autowired
    private CompanyBillAmountPaidRepo companyBillRepo;

    @Autowired
    private CompanyPaymentTimeRepo companyPaymentTimeRepo;

    @Autowired
    private CustomerBillRepo customerBillRepo;

    public Status payVendorBill(String vendorName, BigDecimal amount) {
        CompanyBillAmountPaid bill = companyBillRepo.findByVendorName(vendorName);
        if (bill == null) {
            return new Status(StatusMessage.FAILURE, "No bill record found for vendor: " + vendorName);
        }

        BigDecimal newBalance = bill.getBalance().subtract(amount);
        BigDecimal billPaid=bill.getBillPaid().add(amount);
        bill.setBillPaid(billPaid);
        bill.setBalance(newBalance);

        companyBillRepo.save(bill);

        CompanyPaymentTime paymentTime = new CompanyPaymentTime();
        paymentTime.setVendorName(vendorName);
        paymentTime.setAmountPaid(amount);
        paymentTime.setPaymentTime(LocalDateTime.now());
        companyPaymentTimeRepo.save(paymentTime);

        return new Status(StatusMessage.SUCCESS, "Payment successful. Updated balance: " + newBalance);
    }

    public Status customerPayBill(String name, BigDecimal amount){
        CustomersBill bills = customerBillRepo
                .findTopByCustomerNameOrderByIdDesc(name);

        BigDecimal newBalance = bills.getBalance().subtract(amount);
        // create a new entry
        CustomersBill updated = new CustomersBill();
        updated.setCustomerName(name);
        updated.setBalance(newBalance);
        updated.setBillPaid(amount);  // store how much was deducted
        updated.setPayBillTime(LocalDateTime.now());

        return new Status(StatusMessage.SUCCESS,customerBillRepo.save(updated));
    }

    public Status deleteEntry(int id) {

        CustomersBill entry = customerBillRepo.findById(id);

        CustomersBill bills = customerBillRepo
                .findTopByCustomerNameOrderByIdDesc(entry.getCustomerName());

        // add back deducted amount
        BigDecimal updatedBalance = bills.getBalance().add(entry.getBillPaid());

        // make new balance entry
        CustomersBill revert = new CustomersBill();
        revert.setCustomerName(entry.getCustomerName());
        revert.setBalance(updatedBalance);
        revert.setDeletePayment(entry.getBillPaid());
        revert.setDeletePaymentTime(LocalDateTime.now());
        revert.setBillPaid(BigDecimal.ZERO);
        customerBillRepo.save(revert);

        // delete wrong entry
        customerBillRepo.delete(entry);

        return new Status(StatusMessage.SUCCESS,"The payment entry has been deleted");
    }

}
