package com.example.pos.Service;

import com.example.pos.DTO.ProductDto;
import com.example.pos.entity.pos.*;
import com.example.pos.repo.pos.*;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Autowired
    private ProductNameRepository productNameRepository;

    public Status productAdded(ProductDto productDto) {

        if (productDto == null) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

        // Validate product name
        Optional<ProductName> productNameOpt =
                productNameRepository.findByProductNameAndIsActive(productDto.getProductName(), true);

        if (productNameOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product Name not Found");
        }

        ProductName productName = productNameOpt.get();
        Category category = productName.getCategory();

        if (category == null) {
            return new Status(StatusMessage.FAILURE, "Category not found for the given product");
        }

        // ---------------- PRODUCT TABLE SAVE ----------------
        ProductEntity product = new ProductEntity();
        product.setProductName(String.valueOf(productName.getId()));  // ID stored
        product.setCategory(category.getCategoryName());
        product.setQuantity(productDto.getQuantity());
        product.setPrice(productDto.getPrice());
        product.setVendorName(productDto.getVendorName());
        product.setProductEntryTime(LocalDateTime.now());

        BigDecimal qty = BigDecimal.valueOf(productDto.getQuantity());
        BigDecimal totalPrice = productDto.getPrice().multiply(qty);
        product.setTotalPrice(totalPrice);

        product.setIsActive(true);

        ProductEntity savedProduct = productRepo.save(product);

        // ---------------- INVENTORY TABLE UPDATE/CREATE ----------------
        InventoryEntity inventory = inventoryRepo
                .findByCategoryAndProductName(category.getCategoryName(), productDto.getProductName());
        if (inventory != null) {
            // Update quantity
            int newQty = inventory.getQuantity() + productDto.getQuantity();
            BigDecimal newTotal = productDto.getPrice().multiply(BigDecimal.valueOf(newQty));

            inventory.setQuantity(newQty);
            inventory.setPurchasePrice(productDto.getPrice());
            inventory.setTotalPrice(newTotal);
        } else {
            // Create new inventory
            inventory = new InventoryEntity();
            inventory.setCategory(category.getCategoryName());
            inventory.setProductName(productDto.getProductName());  // IMPORTANT FIX
            inventory.setQuantity(productDto.getQuantity());
            inventory.setPurchasePrice(productDto.getPrice());
            inventory.setTotalPrice(totalPrice);
        }

        inventoryRepo.save(inventory);

        updateCompanyBill(productDto.getVendorName(), totalPrice);


        return new Status(StatusMessage.SUCCESS, savedProduct);
    }

    private void updateCompanyBill(String vendor, BigDecimal totalPrice) {

        YearMonth currentMonth = YearMonth.now();

        // Check if current month bill exists
        CompanyBillAmountPaid currentMonthRecord =
                companyBillRepo.findByVendorNameAndBillingMonth(vendor, currentMonth);

        if (currentMonthRecord != null) {
            // update balance
            BigDecimal updatedBalance = currentMonthRecord.getBalance().add(totalPrice);
            currentMonthRecord.setBalance(updatedBalance);
            companyBillRepo.save(currentMonthRecord);
            return;
        }

        // No current month record → fetch last month’s balance
        CompanyBillAmountPaid lastMonthRecord =
                companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendor);

        BigDecimal carryForwardBalance = (lastMonthRecord != null)
                ? lastMonthRecord.getBalance()
                : BigDecimal.ZERO;

        // Create new monthly bill record
        CompanyBillAmountPaid newBill = new CompanyBillAmountPaid();
        newBill.setVendorName(vendor);
        newBill.setBillPaid(BigDecimal.ZERO);
        newBill.setBillingMonth(currentMonth);
        newBill.setBalance(carryForwardBalance.add(totalPrice));

        companyBillRepo.save(newBill);
    }


    public Status productUpdated(int id, ProductDto productDto) {

        ProductEntity productEntity = productRepo.findById(id);

        if (productEntity == null || Boolean.FALSE.equals(productEntity.getIsActive())) {
            return new Status(StatusMessage.FAILURE, "No record found against this id");
        }

        // ------ Backup old values ------
        String oldProductName = productEntity.getProductName();
        String oldCategory = productEntity.getCategory();
        int oldQuantity = productEntity.getQuantity();
        String oldVendorName = productEntity.getVendorName();
        BigDecimal oldTotalPrice = productEntity.getTotalPrice();

        // Update product table
        ProductEntity updated = updateProductFields(productEntity, productDto);

        // Update inventory
        updateInventory(oldCategory, oldProductName, oldQuantity, updated);

        // Update company bill
        updateCompanyBill(oldVendorName, oldTotalPrice, updated);

        return new Status(StatusMessage.SUCCESS, updated);
    }

    private ProductEntity updateProductFields(ProductEntity product, ProductDto dto) {

        // Resolve product name (if updated)
        if (dto.getProductName() != null) {
            Optional<ProductName> nameOpt =
                    productNameRepository.findByProductNameAndIsActive(dto.getProductName(), true);

            if (nameOpt.isEmpty()) {
                throw new RuntimeException("Product Name not found");
            }
            product.setProductName(String.valueOf(nameOpt.get().getId()));
        }

        if (dto.getCategory() != null) product.setCategory(dto.getCategory());
        if (dto.getQuantity() != null) product.setQuantity(dto.getQuantity());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getVendorName() != null) product.setVendorName(dto.getVendorName());

        // Recalculate total price
        BigDecimal qty = BigDecimal.valueOf(product.getQuantity());
        product.setTotalPrice(product.getPrice().multiply(qty));

        product.setRecordUpdatedTime(LocalDateTime.now());

        return productRepo.save(product);
    }

    private void updateInventory(String oldCategory,
                                 String oldProductName,
                                 int oldQuantity,
                                 ProductEntity updated) {

        // Get old product display name
        Optional<ProductName> oldProd =
                productNameRepository.findProductByIdAndIsActiveTrue(Long.valueOf(oldProductName));

        // STEP 1: SUBTRACT OLD QUANTITY
        InventoryEntity oldInv =
                inventoryRepo.findByCategoryAndProductName(oldCategory, oldProd.get().getProductName());

        if (oldInv != null) {
            int newOldQty = oldInv.getQuantity() - oldQuantity;
            if (newOldQty < 0) newOldQty = 0;

            oldInv.setQuantity(newOldQty);
            oldInv.setTotalPrice(oldInv.getPurchasePrice()
                    .multiply(BigDecimal.valueOf(newOldQty)));

            inventoryRepo.save(oldInv);
        }

        // STEP 2: ADD NEW QUANTITY
        Optional<ProductName> newProd =
                productNameRepository.findProductByIdAndIsActiveTrue(Long.valueOf(updated.getProductName()));

        InventoryEntity newInv =
                inventoryRepo.findByCategoryAndProductName(updated.getCategory(), newProd.get().getProductName());

        if (newInv == null) {
            newInv = new InventoryEntity();
            newInv.setCategory(updated.getCategory());
            newInv.setProductName(newProd.get().getProductName());
            newInv.setQuantity(updated.getQuantity());
            newInv.setPurchasePrice(updated.getPrice());
        } else {
            newInv.setQuantity(newInv.getQuantity() + updated.getQuantity());
            newInv.setPurchasePrice(updated.getPrice());
        }

        newInv.setTotalPrice(newInv.getPurchasePrice()
                .multiply(BigDecimal.valueOf(newInv.getQuantity())));

        inventoryRepo.save(newInv);
    }

    private void updateCompanyBill(String oldVendorName,
                                   BigDecimal oldTotalPrice,
                                   ProductEntity updated) {

        String newVendorName = updated.getVendorName();
        BigDecimal newTotalPrice = updated.getTotalPrice();

        // CASE 1: Vendor changed → subtract old, add new
        if (!oldVendorName.equals(newVendorName)) {
            subtractAmountFromVendor(oldVendorName, oldTotalPrice);
            addAmountToVendor(newVendorName, newTotalPrice);
            return;
        }

        // CASE 2: Same vendor → update difference
        BigDecimal diff = newTotalPrice.subtract(oldTotalPrice);

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            addAmountToVendor(newVendorName, diff);
        } else {
            subtractAmountFromVendor(newVendorName, diff.abs());
        }
    }

    private void addAmountToVendor(String vendor, BigDecimal amount) {

        CompanyBillAmountPaid bill = companyBillRepo.findByVendorName(vendor);

        if (bill == null) {
            bill = new CompanyBillAmountPaid();
            bill.setVendorName(vendor);
            bill.setBillingMonth(YearMonth.now());
            bill.setBalance(amount);
        } else {
            bill.setBillingMonth(YearMonth.now());

            bill.setBalance(bill.getBalance().add(amount));
        }

        companyBillRepo.save(bill);
    }

    private void subtractAmountFromVendor(String vendor, BigDecimal amount) {

        CompanyBillAmountPaid bill = companyBillRepo.findByVendorName(vendor);

        if (bill == null) return;

        bill.setBillingMonth(YearMonth.now());
        BigDecimal updated = bill.getBillPaid().subtract(amount);

// If result becomes negative, set to 0
        if (updated.compareTo(BigDecimal.ZERO) < 0) {
            updated = BigDecimal.ZERO;
        }

        bill.setBillPaid(updated);


        BigDecimal newBalance = bill.getBalance().subtract(amount);
        bill.setBalance(newBalance.max(BigDecimal.ZERO));

        companyBillRepo.save(bill);
    }


    public Status searchProducts(String category, String productName) {

        boolean categoryEmpty = (category == null || category.isBlank());
        boolean productEmpty = (productName == null || productName.isBlank());

        List<ProductEntity> products = new ArrayList<>();

        // Case 0: If BOTH parameters are empty → return ALL products
        if (categoryEmpty && productEmpty) {
            products = productRepo.findAll();

            if (products.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "No products found");
            }

        } else if (!categoryEmpty && !productEmpty) {
            // Case A: both category and productName given
            Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName);
            if (pnOpt.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "Product name not found");
            }
            String pnIdStr = String.valueOf(pnOpt.get().getId());
            products = productRepo.findByCategoryAndProductName(category, pnIdStr);

        } else if (!productEmpty) {
            // Case B: only productName given
            Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName);
            if (pnOpt.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "Product name not found");
            }
            String pnIdStr = String.valueOf(pnOpt.get().getId());
            products = productRepo.findByProductName(pnIdStr);

        } else if (!categoryEmpty) {
            // Case C: only category given
            products = productRepo.findByCategory(category);
        }

        if (products == null || products.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No matching products found");
        }

        // Convert to DTO list with real product name instead of ID
        List<ProductDto> responseList = products.stream().map(p -> {
            ProductDto dto = new ProductDto();

            // Convert stored productName ID to real product name
            String productNameIdStr = p.getProductName();
            String actualProductName = null;

            if (productNameIdStr != null && !productNameIdStr.isBlank()) {
                try {
                    Long pid = Long.valueOf(productNameIdStr);
                    actualProductName = productNameRepository.findById(pid)
                            .map(ProductName::getProductName)
                            .orElse(productNameIdStr);
                } catch (NumberFormatException e) {
                    actualProductName = productNameIdStr;
                }
            }

            dto.setProductName(actualProductName);
            dto.setCategory(p.getCategory());
            dto.setVendorName(p.getVendorName());
            dto.setPrice(p.getPrice());
            dto.setQuantity(p.getQuantity());
            dto.setTotalPrice(p.getTotalPrice());
            dto.setProductEntryTime(p.getProductEntryTime());
            dto.setIsActive(p.getIsActive());
            return dto;

        }).collect(Collectors.toList());

        return new Status(StatusMessage.SUCCESS, responseList);
    }


    public Status deleteRecord(int id) {
        ProductEntity product = productRepo.findById(id);
        if (Boolean.FALSE.equals(product.getIsActive())) {
            return new Status(StatusMessage.FAILURE, "Record is already deleted");
        }
        product.setIsActive(false);
        product.setRecordDeletedTime(LocalDateTime.now());
        productRepo.save(product);
        return new Status(StatusMessage.SUCCESS, "Record is deleted");
    }

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

}
