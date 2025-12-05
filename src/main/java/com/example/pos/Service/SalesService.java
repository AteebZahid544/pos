package com.example.pos.Service;

import com.example.pos.DTO.CustomerBalanceResponseDto;
import com.example.pos.DTO.SalesDto;
import com.example.pos.entity.pos.CustomersBill;
import com.example.pos.entity.pos.InventoryEntity;
import com.example.pos.entity.pos.SalesEntity;
import com.example.pos.repo.pos.CustomerBillRepo;
import com.example.pos.repo.pos.InventoryRepo;
import com.example.pos.repo.pos.SalesRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SalesService {
    @Autowired
    private SalesRepo salesRepo;

    @Autowired
    private CustomerBillRepo customerBillRepo;

    @Autowired
    private InventoryRepo inventoryRepo;

    public Status productSell(SalesDto salesDto) {
        YearMonth currentMonth = YearMonth.now();

        InventoryEntity saleProduct = inventoryRepo.findByCategoryAndProductName(salesDto.getCategory(), salesDto.getProduct());
        if (Objects.isNull(saleProduct)) {
            return new Status(StatusMessage.FAILURE, "This Category or Product is not valid");
        }

        if (Objects.isNull(saleProduct.getQuantity()) || Objects.isNull(salesDto.getQuantity()) ||
                saleProduct.getQuantity() < salesDto.getQuantity()) {
            return new Status(StatusMessage.FAILURE, "You have only : " + saleProduct.getQuantity() + " units of this product in your stock. So this bill is not processed ");
        }

        SalesEntity salesEntity = new SalesEntity();
        salesEntity.setProduct(salesDto.getProduct());
        Integer setQuantity = salesDto.getQuantity();
        salesEntity.setQuantity(setQuantity);
        salesEntity.setPrice(salesDto.getPrice());
        salesEntity.setDiscount(salesDto.getDiscount());
        salesEntity.setCategory(salesDto.getCategory());
        salesEntity.setCustomerName(salesDto.getCustomerName());
        salesEntity.setSellTime(LocalDateTime.now());

        BigDecimal discount = salesDto.getDiscount();
        BigDecimal totalQuantity = BigDecimal.valueOf(salesDto.getQuantity());
        BigDecimal total = (salesDto.getPrice().multiply(totalQuantity)).subtract(discount);
        salesEntity.setTotalAmount(total);

        BigDecimal amountPaid = salesDto.getAmountPaid(); // may be null

        Integer currentQuantity = saleProduct.getQuantity();

        if (currentQuantity == null) {
            return new Status(StatusMessage.FAILURE, "Product quantity is not available.");
        }

        Integer newQuantity = currentQuantity - setQuantity;
        if (newQuantity < 0) {
            return new Status(StatusMessage.FAILURE, "Not enough stock to fulfill the sale.");
        }

        saleProduct.setQuantity(newQuantity);
        inventoryRepo.save(saleProduct);

        SalesEntity save=salesRepo.save(salesEntity);
        String sellTime= String.valueOf(save.getSellTime());

        appendCustomerLedgerEntry(salesDto.getCustomerName(), total, amountPaid,sellTime);
        return new Status(StatusMessage.SUCCESS, save);
    }

    private void appendCustomerLedgerEntry(String customerName, BigDecimal saleTotal, BigDecimal amountPaid, String sellTime) {

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime now = LocalDateTime.now();

        // Normalize inputs
        BigDecimal sale = saleTotal == null ? BigDecimal.ZERO : saleTotal;
        BigDecimal paid = amountPaid == null ? BigDecimal.ZERO : amountPaid;

        // Fetch last balance
        CustomersBill last = customerBillRepo.findTopByCustomerNameOrderByIdDesc(customerName);

        BigDecimal oldBalance = (last == null || last.getBalance() == null)
                ? BigDecimal.ZERO
                : last.getBalance();

        // FINAL BALANCE
        BigDecimal finalBalance = oldBalance.add(sale).subtract(paid);

        if (finalBalance.compareTo(BigDecimal.ZERO) < 0) {
            finalBalance = BigDecimal.ZERO;
        }

        CustomersBill entry = new CustomersBill();
        entry.setCustomerName(customerName);
        entry.setBillingMonth(currentMonth);
        entry.setTotalAmount(saleTotal);
        entry.setBalance(finalBalance);
        entry.setBillPaid(paid);

        // ------------------------------------------
        // 🔥 NEW REQUIRED LOGIC
        // ------------------------------------------

        if (Objects.isNull(paid) || paid.compareTo(BigDecimal.ZERO) == 0) {
            // ✔ Customer did NOT pay → store billTime
            entry.setBillTime(sellTime);
            entry.setPayBillTime(null);
        } else {
            // ✔ Customer PAID → store payBillTime
            entry.setPayBillTime(now);
            entry.setBillTime(sellTime);
        }

        customerBillRepo.save(entry);
    }



    @Transactional
    public Status cancelProductSale(int id) {
        Optional<SalesEntity> optionalSale = salesRepo.findById(id);
        if (optionalSale.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Sale record not found");
        }

        SalesEntity sale = optionalSale.get();
        InventoryEntity products = inventoryRepo.findByCategoryAndProductName(sale.getCategory(), sale.getProduct());

        if (products == null) {
            return new Status(StatusMessage.FAILURE, "Product not found");
        }

        Integer restoredQuantity = products.getQuantity() + sale.getQuantity();
        products.setQuantity(restoredQuantity);
        inventoryRepo.save(products);

        salesRepo.deleteById(id);

        return new Status(StatusMessage.SUCCESS, "Sale cancelled and stock restored successfully");
    }

    public Status getCustomerBalance(String customerName) {

        // ================================
        // 1. SINGLE CUSTOMER BALANCE CASE
        // ================================
        if (customerName != null && !customerName.trim().isEmpty()) {

            CustomersBill latest = customerBillRepo.findTopByCustomerNameOrderByIdDesc(customerName);

            if (latest == null) {
                return new Status(StatusMessage.FAILURE, "No balance found for customer: " + customerName);
            }

            // Map entity → DTO
            CustomerBalanceResponseDto dto = new CustomerBalanceResponseDto();
            dto.setCustomerName(latest.getCustomerName());
            dto.setCustomerBalance(latest.getBalance());

            return new Status(StatusMessage.SUCCESS, dto);
        }


        // ==================================
        // 2. ALL CUSTOMERS LATEST BALANCES
        // ==================================
        List<CustomersBill> balances = customerBillRepo.findLatestBalanceForAllCustomers();

        if (balances == null || balances.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No customer balances found");
        }

        // Convert list → List<CustomerBalanceResponseDto>
        List<CustomerBalanceResponseDto> responseDtoList = balances.stream().map(c -> {

            CustomerBalanceResponseDto dto = new CustomerBalanceResponseDto();
            dto.setCustomerName(c.getCustomerName());
            dto.setCustomerBalance(c.getBalance());

            return dto;

        }).collect(Collectors.toList());

        return new Status(StatusMessage.SUCCESS, responseDtoList);
    }

    public Status getCustomerLedger(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Customer name is required");
        }

        List<CustomersBill> ledger = customerBillRepo.findByCustomerName(customerName);

        if (ledger.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No records found for customer: " + customerName);
        }

        return new Status(StatusMessage.SUCCESS, ledger);
    }
}
