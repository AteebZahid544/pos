package com.example.pos.Service;

import com.example.pos.DTO.ProductDto;
import com.example.pos.entity.CompanyBillAmountPaid;
import com.example.pos.entity.CompanyPaymentTime;
import com.example.pos.entity.InventoryEntity;
import com.example.pos.entity.ProductEntity;
import com.example.pos.repo.CompanyBillAmountPaidRepo;
import com.example.pos.repo.CompanyPaymentTimeRepo;
import com.example.pos.repo.InventoryRepo;
import com.example.pos.repo.ProductRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private InventoryRepo inventoryRepo;
    @Autowired
    private CompanyBillAmountPaidRepo companyBillRepo;

    @Autowired
    private CompanyPaymentTimeRepo companyPaymentTimeRepo;

    public Status productAdded(ProductDto productDto) {
        if (Objects.nonNull(productDto)) {
            // Step 1: Save Product
            ProductEntity product = new ProductEntity();
            product.setCategory(productDto.getCategory());
            product.setQuantity(productDto.getQuantity());
            product.setPrice(productDto.getPrice());
            product.setVendorName(productDto.getVendorName());
            product.setProductEntryTime(LocalDateTime.now());
            BigDecimal quantity = BigDecimal.valueOf(productDto.getQuantity());
            BigDecimal totalPrice = productDto.getPrice().multiply(quantity);
            product.setTotalPrice(totalPrice);
            product.setIsActive(true);

            ProductEntity savedProduct = productRepo.save(product);

            // Step 2: Fetch inventory record by category
            List<InventoryEntity> existingInventoryList = inventoryRepo.findAll().stream()
                    .filter(inv -> inv.getCategory().equals(productDto.getCategory()) && inv.getVendorName().equals(productDto.getVendorName()))
                    .collect(Collectors.toList());

            if (!existingInventoryList.isEmpty()) {
                // Category exists → update existing record
                InventoryEntity inventory = existingInventoryList.get(0);
                int updatedQuantity = inventory.getQuantity() + productDto.getQuantity();
                BigDecimal updatedTotalPrice = productDto.getPrice().multiply(BigDecimal.valueOf(updatedQuantity));

                inventory.setPurchasePrice(productDto.getPrice());
                inventory.setQuantity(updatedQuantity);
                inventory.setTotalPrice(updatedTotalPrice);
                inventoryRepo.save(inventory);

            } else {
                // Category doesn't exist → create new record
                InventoryEntity inventory = new InventoryEntity();
                inventory.setQuantity(productDto.getQuantity());
                inventory.setPurchasePrice(productDto.getPrice());
                inventory.setTotalPrice(totalPrice);
                inventory.setVendorName(productDto.getVendorName());
                inventory.setCategory(productDto.getCategory());

                inventoryRepo.save(inventory);
            }
            String vendor = productDto.getVendorName();
            YearMonth currentMonth = YearMonth.now();

// Check if there's already a record for this vendor in the current month
            CompanyBillAmountPaid currentMonthRecord = companyBillRepo.findByVendorNameAndBillingMonth(vendor, currentMonth);

            if (currentMonthRecord != null) {
                int updatedBalance = currentMonthRecord.getBalance() + totalPrice.intValue();
                currentMonthRecord.setBalance(updatedBalance);
                companyBillRepo.save(currentMonthRecord);
            } else {

                CompanyBillAmountPaid lastMonthRecord = companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendor);

                int carryForwardBalance = (lastMonthRecord != null) ? lastMonthRecord.getBalance() : 0;

                CompanyBillAmountPaid newBill = new CompanyBillAmountPaid();
                newBill.setVendorName(vendor);
                newBill.setAmountPaid(0); // No bill paid yet
                newBill.setBillingMonth(currentMonth);
                newBill.setBalance(carryForwardBalance + totalPrice.intValue()); // Carry forward + current
                companyBillRepo.save(newBill);
            }


            return new Status(StatusMessage.SUCCESS, savedProduct);
        } else {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }
    }


    public Status productUpdated(String category, ProductDto productDto) {
        ProductEntity product = productRepo.findByCategory(category);

        if (product == null || Boolean.FALSE.equals(product.getIsActive())) {
            return new Status(StatusMessage.FAILURE, "Category does not exist against this name");
        }

        // Update only if the value is provided in the DTO
        if (Objects.nonNull(productDto.getCategory())) {
            product.setCategory(productDto.getCategory());
        }

        if (Objects.nonNull(productDto.getQuantity())) {
            product.setQuantity(productDto.getQuantity());
        }

        if (Objects.nonNull(productDto.getPrice())) {
            product.setPrice(productDto.getPrice());
        }

        // Recalculate total price if both price and quantity are now available
        if (Objects.nonNull(product.getPrice()) && Objects.nonNull(product.getQuantity())) {
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            BigDecimal totalPrice = product.getPrice().multiply(quantity);
            product.setTotalPrice(totalPrice);
        }

        return new Status(StatusMessage.SUCCESS, productRepo.save(product));
    }

    public Status getProductsByCategory(String category) {
        List<ProductEntity> allProducts = productRepo.findAll();

        if (allProducts.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No products found");
        }

        List<ProductEntity> filteredProducts = allProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .filter(p -> category == null || category.equalsIgnoreCase(p.getCategory()))
                .collect(Collectors.toList());

        if (filteredProducts.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No active product found" +
                    (category != null ? " for category: " + category : ""));
        }

        return new Status(StatusMessage.SUCCESS, filteredProducts);
    }


    public Status deleteRecord(int id) {
        ProductEntity product=productRepo.findById(id);
        if (Boolean.FALSE.equals(product.getIsActive())){
            return new Status(StatusMessage.FAILURE,"Record is already deleted");
        }
        product.setIsActive(false);
        productRepo.save(product);
        return new Status(StatusMessage.SUCCESS,"Product is deleted");
    }

    public Status payVendorBill(String vendorName, int amount) {
        CompanyBillAmountPaid bill = companyBillRepo.findByVendorName(vendorName);
        if (bill == null) {
            return new Status(StatusMessage.FAILURE, "No bill record found for vendor: " + vendorName);
        }

        int newBalance = bill.getBalance() - amount;

        bill.setAmountPaid(bill.getAmountPaid() + amount);
        bill.setBalance(newBalance);

        companyBillRepo.save(bill);

        CompanyPaymentTime paymentTime = new CompanyPaymentTime();
        paymentTime.setVendorName(vendorName);
        paymentTime.setAmountPaid(amount);
        paymentTime.setPaymentTime(LocalDateTime.now());
        companyPaymentTimeRepo.save(paymentTime);

        return new Status(StatusMessage.SUCCESS, "Payment successful. Updated balance: " + newBalance);
    }

}
