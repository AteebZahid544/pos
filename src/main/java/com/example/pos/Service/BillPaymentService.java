package com.example.pos.Service;

import com.example.pos.entity.pos.*;
import com.example.pos.repo.pos.*;

import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.hibernate.query.sql.internal.ParameterRecognizerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class BillPaymentService {

    @Autowired
    private CompanyBillAmountPaidRepo companyBillRepo;

    @Autowired
    private CompanyPaymentTimeRepo companyPaymentTimeRepo;

    @Autowired
    private CustomerBillAmountPaidRepo customerBillRepo;

    @Autowired

    private CustomerPaymentTimeRepo customerPaymentTimeRepo;

    @Autowired
    private CompanyInvoiceAmountRepo companyInvoiceAmountRepo;

    @Autowired
    private CustomerInvoiceRecordRepo customerInvoiceRecordRepo;

    public Status payVendorBill(String vendorName, BigDecimal amount) {
        YearMonth currentMonth = YearMonth.now();
        CompanyBillAmountPaid bill = companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendorName);
        if (bill == null) {
            return new Status(StatusMessage.FAILURE, "No bill record found for vendor: " + vendorName);
        }

        BigDecimal newBalance = bill.getBalance().subtract(amount);
        bill.setBalance(newBalance);

        companyBillRepo.save(bill);

        CompanyPaymentTime paymentTime = new CompanyPaymentTime();
        paymentTime.setVendorName(vendorName);
        paymentTime.setAmountPaid(amount);
        paymentTime.setPaymentTime(LocalDateTime.now());
        paymentTime.setBillingMonth(currentMonth);
        paymentTime.setIsActive(true);
        companyPaymentTimeRepo.save(paymentTime);

        return new Status(StatusMessage.SUCCESS, "Payment successful. Updated balance: " + newBalance);
    }

    public Status customerPayBill(String name, BigDecimal amount) {
        YearMonth currentMonth = YearMonth.now();

        CustomerBillAmountPaid bills = customerBillRepo
                .findTopByCustomerNameOrderByBillingMonthDesc(name);

        BigDecimal newBalance = bills.getBalance().subtract(amount);

        bills.setBalance(newBalance);
        customerBillRepo.save(bills);

        CustomerPaymentTime paymentTime = new CustomerPaymentTime();
        paymentTime.setCustomerName(name);
        paymentTime.setAmountPaid(amount);
        paymentTime.setPaymentTime(LocalDateTime.now());
        paymentTime.setBillingMonth(currentMonth);
        paymentTime.setIsActive(true);
        customerPaymentTimeRepo.save(paymentTime);

        return new Status(StatusMessage.SUCCESS, "Payment successful. Updated balance: " + newBalance);
    }

    public Status getVendorBalance(String vendorName) {
        CompanyBillAmountPaid companyBillAmountPaid = companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendorName);
        if (companyBillAmountPaid == null) {
            return new Status(StatusMessage.SUCCESS, "No Balance found");
        }
        return new Status(StatusMessage.SUCCESS, companyBillAmountPaid.getBalance());

    }

    public Status getCustomerBalance(String customerName) {
        CustomerBillAmountPaid customerBillAmountPaid = customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(customerName);
        if (customerBillAmountPaid == null) {
            return new Status(StatusMessage.SUCCESS, "No Balance found");
        }
        return new Status(StatusMessage.SUCCESS, customerBillAmountPaid.getBalance());

    }

    @Transactional
    public Status updatePayBill(int invoiceNumber,
                                String status,
                                BigDecimal newAmountPaid,
                                String vendorName,
                                LocalDateTime paymentTimeFrontEnd) {

        // =========================
        // CASE 2: invoiceNumber = 0
        // =========================
        if (invoiceNumber == 0) {

            // Find payment time record by vendor and exact payment time
            CompanyPaymentTime paymentTimeRecord =
                    companyPaymentTimeRepo.findByVendorNameAndPaymentTime(vendorName, paymentTimeFrontEnd);
            if (paymentTimeRecord==null){
                return new Status(StatusMessage.FAILURE,"This Record is already deleted");
            }

            BigDecimal oldAmountPaid =
                    paymentTimeRecord.getAmountPaid() == null
                            ? BigDecimal.ZERO
                            : paymentTimeRecord.getAmountPaid();

            BigDecimal difference = newAmountPaid.subtract(oldAmountPaid);

            // Update payment time record
            paymentTimeRecord.setAmountPaid(newAmountPaid);
            paymentTimeRecord.setPaymentTime(LocalDateTime.now());
            companyPaymentTimeRepo.save(paymentTimeRecord);

            // Update company bill balance
            CompanyBillAmountPaid bill =
                    companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendorName);

            bill.setBalance(bill.getBalance().subtract(difference));
            companyBillRepo.save(bill);

            return new Status(StatusMessage.SUCCESS, "Payment updated successfully");
        }

        // =========================
        // CASE 1: invoiceNumber > 0
        // =========================
        if (invoiceNumber > 0 && "Purchase".equalsIgnoreCase(status)) {


            CompanyInvoiceAmount invoice =
                    companyInvoiceAmountRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status,true)
                            .orElseThrow(() -> new RuntimeException("Invoice not found"));

            BigDecimal oldAmountPaid =
                    invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();

            BigDecimal difference = newAmountPaid.subtract(oldAmountPaid);

            // Update invoice
            invoice.setAmountPaid(newAmountPaid);
            invoice.setGrandTotal(invoice.getGrandTotal().subtract(difference));
            companyInvoiceAmountRepo.save(invoice);

            // Update bill balance
            CompanyBillAmountPaid bill =
                    companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(invoice.getVendorName());

            bill.setBalance(bill.getBalance().subtract(difference));
            companyBillRepo.save(bill);

            // Update payment time
            CompanyPaymentTime paymentTime =
                    companyPaymentTimeRepo.findByInvoiceNumberAndIsActive(invoiceNumber,true)
                            .orElse(new CompanyPaymentTime());

            paymentTime.setInvoiceNumber(invoiceNumber);
            paymentTime.setVendorName(invoice.getVendorName());
            paymentTime.setAmountPaid(newAmountPaid);
            paymentTime.setPaymentTime(LocalDateTime.now());
            paymentTime.setBillingMonth(YearMonth.now());
            paymentTime.setIsActive(true);

            companyPaymentTimeRepo.save(paymentTime);

        }
        return new Status(StatusMessage.SUCCESS,"PayBill updated successfully");

}

    public Status deleteInvoice(int invoiceNumber, String status, LocalDateTime payTime, String vendorName) {

        if (invoiceNumber > 0 && "Purchase".equalsIgnoreCase(status)) {

        CompanyInvoiceAmount invoice =
                companyInvoiceAmountRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status,true)
                        .orElseThrow(() -> new RuntimeException("Invoice not found"));

        BigDecimal paidAmount =
                invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();

        // 🔹 Reverse vendor balance
        CompanyBillAmountPaid bill =
                companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(invoice.getVendorName());

        bill.setBalance(bill.getBalance().add(paidAmount));
        companyBillRepo.save(bill);

        BigDecimal newPaidAmount= invoice.getAmountPaid().subtract(paidAmount);
        BigDecimal newGrandTotal= invoice.getGrandTotal().subtract(paidAmount);
        invoice.setAmountPaid(newPaidAmount);
        invoice.setGrandTotal(newGrandTotal);
        companyInvoiceAmountRepo.save(invoice);

        CompanyPaymentTime companyPaymentTime= companyPaymentTimeRepo.findByVendorNameAndPaymentTime(bill.getVendorName(),payTime);

        if (companyPaymentTime!=null){
            companyPaymentTime.setIsActive(false);
            companyPaymentTimeRepo.save(companyPaymentTime);
        }

}else {
            CompanyPaymentTime companyPaymentTime=companyPaymentTimeRepo.findByVendorNameAndPaymentTime(vendorName,payTime);
            if (companyPaymentTime!=null){
                companyPaymentTime.setIsActive(false);
                companyPaymentTimeRepo.save(companyPaymentTime);
            }
            BigDecimal oldPayment= companyPaymentTime.getAmountPaid() == null ? BigDecimal.ZERO : companyPaymentTime.getAmountPaid();

            CompanyBillAmountPaid companyBillAmountPaid= companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendorName);
            BigDecimal newBalance= companyBillAmountPaid.getBalance().add(oldPayment);
            companyBillAmountPaid.setBalance(newBalance);
            companyBillRepo.save(companyBillAmountPaid);
        }
        return new Status(StatusMessage.SUCCESS,"Invoice deleted successfully");
}


    @Transactional
    public Status updateCustomerPayBill(int invoiceNumber,
                                String status,
                                BigDecimal newAmountPaid,
                                String customerName,
                                LocalDateTime paymentTimeFrontEnd) {

        // =========================
        // CASE 2: invoiceNumber = 0
        // =========================
        if (invoiceNumber == 0) {

            // Find payment time record by vendor and exact payment time
            CustomerPaymentTime paymentTimeRecord =
                    customerPaymentTimeRepo.findByCustomerNameAndPaymentTime(customerName, paymentTimeFrontEnd);
            if (paymentTimeRecord==null){
                return new Status(StatusMessage.FAILURE,"This Record is already deleted");
            }

            BigDecimal oldAmountPaid =
                    paymentTimeRecord.getAmountPaid() == null
                            ? BigDecimal.ZERO
                            : paymentTimeRecord.getAmountPaid();

            BigDecimal difference = newAmountPaid.subtract(oldAmountPaid);

            // Update payment time record
            paymentTimeRecord.setAmountPaid(newAmountPaid);
            paymentTimeRecord.setPaymentTime(LocalDateTime.now());
            customerPaymentTimeRepo.save(paymentTimeRecord);

            // Update company bill balance
            CustomerBillAmountPaid bill =
                    customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(customerName);

            bill.setBalance(bill.getBalance().subtract(difference));
            customerBillRepo.save(bill);

            return new Status(StatusMessage.SUCCESS, "Payment updated successfully");
        }

        // =========================
        // CASE 1: invoiceNumber > 0
        // =========================
        if (invoiceNumber > 0 && "Sale".equalsIgnoreCase(status)) {


            CustomerInvoiceRecord invoice =
                    customerInvoiceRecordRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status,true)
                            .orElseThrow(() -> new RuntimeException("Invoice not found"));

            BigDecimal oldAmountPaid =
                    invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();

            BigDecimal difference = newAmountPaid.subtract(oldAmountPaid);

            // Update invoice
            invoice.setAmountPaid(newAmountPaid);
            invoice.setGrandTotal(invoice.getGrandTotal().subtract(difference));
            customerInvoiceRecordRepo.save(invoice);

            // Update bill balance
            CustomerBillAmountPaid bill =
                    customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(invoice.getCustomerName());

            bill.setBalance(bill.getBalance().subtract(difference));
            customerBillRepo.save(bill);

            // Update payment time
            CustomerPaymentTime paymentTime =
                    customerPaymentTimeRepo.findByInvoiceNumberAndIsActive(invoiceNumber,true)
                            .orElse(new CustomerPaymentTime());

            paymentTime.setInvoiceNumber(invoiceNumber);
            paymentTime.setCustomerName(invoice.getCustomerName());
            paymentTime.setAmountPaid(newAmountPaid);
            paymentTime.setPaymentTime(LocalDateTime.now());
            paymentTime.setBillingMonth(YearMonth.now());
            paymentTime.setIsActive(true);

            customerPaymentTimeRepo.save(paymentTime);

        }
        return new Status(StatusMessage.SUCCESS,"PayBill updated successfully");

    }

    public Status deleteCustomerInvoice(int invoiceNumber, String status, LocalDateTime payTime, String customerName) {

        if (invoiceNumber > 0 && "Sale".equalsIgnoreCase(status)) {

            CustomerInvoiceRecord invoice =
                    customerInvoiceRecordRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status,true)
                            .orElseThrow(() -> new RuntimeException("Invoice not found"));

            BigDecimal paidAmount =
                    invoice.getAmountPaid() == null ? BigDecimal.ZERO : invoice.getAmountPaid();

            // 🔹 Reverse vendor balance
            CustomerBillAmountPaid bill =
                    customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(invoice.getCustomerName());

            bill.setBalance(bill.getBalance().add(paidAmount));
            customerBillRepo.save(bill);

            BigDecimal newPaidAmount= invoice.getAmountPaid().subtract(paidAmount);
            BigDecimal newGrandTotal= invoice.getGrandTotal().subtract(paidAmount);
            invoice.setAmountPaid(newPaidAmount);
            invoice.setGrandTotal(newGrandTotal);
            customerInvoiceRecordRepo.save(invoice);

            CustomerPaymentTime customerPaymentTime= customerPaymentTimeRepo.findByCustomerNameAndPaymentTime(bill.getCustomerName(),payTime);

            if (customerPaymentTime!=null){
                customerPaymentTime.setIsActive(false);
                customerPaymentTimeRepo.save(customerPaymentTime);
            }

        }else {
            CustomerPaymentTime customerPaymentTime= customerPaymentTimeRepo.findByCustomerNameAndPaymentTime(customerName,payTime);
            if (customerPaymentTime!=null){
                customerPaymentTime.setIsActive(false);
                customerPaymentTimeRepo.save(customerPaymentTime);
            }
            BigDecimal oldPayment= customerPaymentTime.getAmountPaid() == null ? BigDecimal.ZERO : customerPaymentTime.getAmountPaid();

            CustomerBillAmountPaid customerBillAmountPaid= customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(customerName);
            BigDecimal newBalance= customerBillAmountPaid.getBalance().add(oldPayment);
            customerBillAmountPaid.setBalance(newBalance);
            customerBillRepo.save(customerBillAmountPaid);
        }
        return new Status(StatusMessage.SUCCESS,"Invoice deleted successfully");
    }
}
