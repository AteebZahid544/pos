package com.example.pos.Service;

import com.example.pos.DTO.*;
import com.example.pos.entity.pos.*;
import com.example.pos.repo.pos.*;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
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

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CompanyBillAmountPaidRepo companyBillAmountPaidRepo;

    @Autowired
    private CompanyInvoiceAmountRepo companyInvoiceAmountRepo;

    @Value("${invoice.upload.path}")
    private String invoiceUploadPath;

    @Transactional
    public Status productAdded(List<ProductDto> productDtos,
                               BigDecimal invoiceDiscount,
                               BigDecimal invoiceRent,
                               String description,
                               String vendorName,
                               BigDecimal payBill,
                               String status,
                               BigDecimal gstPercentage) { // 🔴 NEW: Add GST parameter

        YearMonth currentMonth = YearMonth.now();

        if (productDtos == null || productDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

        List<ProductEntity> savedProducts = new ArrayList<>();
        BigDecimal invoiceTotal = BigDecimal.ZERO; // sum of all product totals
        int invoiceNumber = productDtos.get(0).getInvoiceNumber(); // assume all products have same invoice number

        // --- Save products and calculate invoice total ---
        for (ProductDto productDto : productDtos) {
            Optional<ProductName> productNameOpt =
                    productNameRepository.findByProductNameAndIsActive(productDto.getProductName(), true);

            if (productNameOpt.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "Product Name : " + productDto.getProductName() + " not found on your record");
            }

            ProductName productName = productNameOpt.get();
            Category category = productName.getCategory();
            if (category == null) {
                return new Status(StatusMessage.FAILURE, "Category not found for the given product: " + productDto.getProductName());
            }

            ProductEntity product = new ProductEntity();
            product.setProductName(productDto.getProductName());
            product.setCategory(category.getCategoryName());
            product.setQuantity(productDto.getQuantity());
            product.setPrice(productDto.getPrice());
            product.setProductEntryTime(LocalDateTime.now());
            product.setInvoiceNumber(invoiceNumber);

            BigDecimal totalPrice;
            if ("packet".equalsIgnoreCase(category.getCategoryName())) {
                Integer quantity = productDto.getQuantity();
                BigDecimal size = productDto.getSize() != null ? productDto.getSize() : BigDecimal.ONE;
                BigDecimal ktae = productDto.getKtae() != null ? productDto.getKtae() : BigDecimal.ONE;
                BigDecimal gram = productDto.getGram() != null ? productDto.getGram() : BigDecimal.ONE;
                totalPrice = BigDecimal.valueOf(quantity)
                        .multiply(size)
                        .multiply(ktae)
                        .multiply(gram)
                        .multiply(productDto.getPrice())
                        .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

                product.setSize(size);
                product.setKtae(ktae);
                product.setGram(gram);
            } else if ("roll".equalsIgnoreCase(category.getCategoryName()) || ("reel".equalsIgnoreCase(category.getCategoryName()))) {
                BigDecimal size = productDto.getSize() != null ? productDto.getSize() : BigDecimal.ONE;
                totalPrice = size.multiply(BigDecimal.valueOf(productDto.getQuantity())).multiply(productDto.getPrice());
                product.setSize(size);
            } else {
                totalPrice = productDto.getPrice().multiply(BigDecimal.valueOf(productDto.getQuantity()));
            }

            product.setTotalPrice(totalPrice);
            product.setStatus("Purchase");
            product.setIsActive(true);
            ProductEntity savedProduct = productRepo.save(product);
            savedProducts.add(savedProduct);

            // For each product in the invoice
            InventoryEntity inventory = inventoryRepo.findByCategoryAndProductNameAndAddedMonth(category.getCategoryName(), productDto.getProductName(),currentMonth);

            if (inventory != null) {
                // Check if inventory for current month exists
                InventoryEntity currentMonthInventory = inventoryRepo
                        .findByCategoryAndProductNameAndAddedMonth(
                                category.getCategoryName(),
                                productDto.getProductName(),
                                currentMonth
                        );

                if (currentMonthInventory == null) {
                    // Carry over last month's stock
                    YearMonth lastMonth = currentMonth.minusMonths(1);

                    InventoryEntity lastMonthInventory = inventoryRepo
                            .findByCategoryAndProductNameAndAddedMonth(
                                    category.getCategoryName(),
                                    productDto.getProductName(),
                                    lastMonth
                            );

                    currentMonthInventory = new InventoryEntity();
                    currentMonthInventory.setCategory(category.getCategoryName());
                    currentMonthInventory.setProductName(productDto.getProductName());
                    currentMonthInventory.setQuantity(lastMonthInventory != null ? lastMonthInventory.getQuantity() : 0);
                    currentMonthInventory.setPurchasePrice(productDto.getPrice());
                    currentMonthInventory.setTotalPrice(calculateInventoryTotal(currentMonthInventory));
                    currentMonthInventory.setSize(productDto.getSize());
                    currentMonthInventory.setKtae(productDto.getKtae());
                    currentMonthInventory.setGram(productDto.getGram());
                    currentMonthInventory.setAddedMonth(currentMonth);
                }

                // Add current purchase quantity
                currentMonthInventory.setQuantity(currentMonthInventory.getQuantity() + productDto.getQuantity());
                currentMonthInventory.setPurchasePrice(productDto.getPrice());
                currentMonthInventory.setTotalPrice(calculateInventoryTotal(currentMonthInventory));

                // ✅ Save only current month inventory
                inventoryRepo.save(currentMonthInventory);

            } else {
                YearMonth lastMonth = currentMonth.minusMonths(1);

                InventoryEntity lastMonthInventory = inventoryRepo
                        .findByCategoryAndProductNameAndAddedMonth(
                                category.getCategoryName(),
                                productDto.getProductName(),
                                lastMonth
                        );
                // No inventory yet, create new
                inventory = new InventoryEntity();
                inventory.setCategory(category.getCategoryName());
                inventory.setProductName(productDto.getProductName());
                if (lastMonthInventory != null && lastMonthInventory.getQuantity()>0){
                    inventory.setQuantity(lastMonthInventory.getQuantity() +productDto.getQuantity());}
                else {
                    inventory.setQuantity(productDto.getQuantity());
                }
                inventory.setPurchasePrice(productDto.getPrice());
                inventory.setSize(productDto.getSize());
                inventory.setKtae(productDto.getKtae());
                inventory.setGram(productDto.getGram());
                inventory.setTotalPrice(calculateInventoryTotal(inventory));

                inventory.setAddedMonth(currentMonth);
                inventoryRepo.save(inventory);
            }

            invoiceTotal = invoiceTotal.add(totalPrice); // add product total
        }

        // --- Apply invoice-level discount and rent ---
        if (invoiceDiscount != null) invoiceTotal = invoiceTotal.subtract(invoiceDiscount);
        if (invoiceRent != null) invoiceTotal = invoiceTotal.add(invoiceRent);

        // 🔴 GST CALCULATION (ADDED - OPTIONAL)
        BigDecimal gstAmount = BigDecimal.ZERO;
        if (gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
            gstAmount = invoiceTotal.multiply(gstPercentage)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            invoiceTotal = invoiceTotal.add(gstAmount);
        }
        // 🔴 END GST CALCULATION

        if (payBill != null) invoiceTotal = invoiceTotal.subtract(payBill);

        // --- Save invoice summary in CompanyInvoiceAmount ---
        CompanyInvoiceAmount invoiceAmount = new CompanyInvoiceAmount();
        invoiceAmount.setInvoiceNumber(invoiceNumber);
        invoiceAmount.setGrandTotal(invoiceTotal);
        invoiceAmount.setDiscount(invoiceDiscount != null ? invoiceDiscount : BigDecimal.ZERO);
        invoiceAmount.setRent(invoiceRent != null ? invoiceRent : BigDecimal.ZERO);
        invoiceAmount.setDescription(description);
        if (payBill != null) {
            invoiceAmount.setAmountPaid(payBill);
        }
        invoiceAmount.setVendorName(vendorName);
        invoiceAmount.setStatus("Purchase");
        invoiceAmount.setIsActive(true);
        invoiceAmount.setBillingMonth(currentMonth);
        invoiceAmount.setPurchaseDate(LocalDateTime.now());
        invoiceAmount.setInvoiceDate(LocalDateTime.now());

        // 🔴 SET GST FIELDS (ADDED)
        if (gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
            invoiceAmount.setGstPercentage(gstPercentage);
            invoiceAmount.setGstAmount(gstAmount);
            // Calculate and store total before GST
            BigDecimal totalBeforeGst = invoiceTotal.subtract(gstAmount);
            if (payBill != null) {
                totalBeforeGst = totalBeforeGst.add(payBill); // Add back payment to get original subtotal
            }
            invoiceAmount.setTotalBeforeGst(totalBeforeGst);
        } else {
            // Set defaults if no GST
            invoiceAmount.setGstPercentage(BigDecimal.ZERO);
            invoiceAmount.setGstAmount(BigDecimal.ZERO);
            invoiceAmount.setTotalBeforeGst(invoiceTotal.add(payBill != null ? payBill : BigDecimal.ZERO));
        }
        // 🔴 END GST FIELDS

        companyInvoiceAmountRepo.save(invoiceAmount);

        // Update company/vendor bill with final total (including GST if applicable)
        updateCompanyBill(vendorName, invoiceTotal, status, null);

        CompanyPaymentTime payment = new CompanyPaymentTime();
        payment.setInvoiceNumber(invoiceNumber);
        payment.setVendorName(vendorName);
        payment.setAmountPaid(payBill);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setBillingMonth(currentMonth);
        payment.setIsActive(true);

        companyPaymentTimeRepo.save(payment);

        return new Status(StatusMessage.SUCCESS, "Products and invoice saved successfully");
    }
    public void saveInvoiceImage(Integer invoiceNumber, String status, MultipartFile file) throws IOException {

        // Base folder (from your config)
        File baseDir = new File(invoiceUploadPath);

        // Create a subfolder by status (e.g., D:/pos/invoices/purchase)
        File statusDir = new File(baseDir, status);
        if (!statusDir.exists()) {
            boolean created = statusDir.mkdirs(); // creates all missing directories
            if (!created) {
                throw new IOException("Failed to create directory: " + statusDir.getAbsolutePath());
            }
        }

        // Build unique file name
        String fileName = "INV_" + invoiceNumber + "_" + System.currentTimeMillis()
                + "_" + file.getOriginalFilename();

        // Full path to save the file
        File destinationFile = new File(statusDir, fileName);

        // Copy file
        Files.copy(file.getInputStream(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Save file path to DB
        CompanyInvoiceAmount invoice = companyInvoiceAmountRepo
                .findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setInvoiceImagePath(destinationFile.getAbsolutePath());
        companyInvoiceAmountRepo.save(invoice);
    }

    private BigDecimal calculateInventoryTotal(InventoryEntity inventory) {

        String category = inventory.getCategory();
        BigDecimal price = inventory.getPurchasePrice();
        int quantity = inventory.getQuantity();

        BigDecimal size = inventory.getSize() != null ? inventory.getSize() : BigDecimal.ONE;
        BigDecimal ktae = inventory.getKtae() != null ? inventory.getKtae() : BigDecimal.ONE;
        BigDecimal gram = inventory.getGram() != null ? inventory.getGram() : BigDecimal.ONE;

        if ("packet".equalsIgnoreCase(category)) {

            return BigDecimal.valueOf(quantity)
                    .multiply(size)
                    .multiply(ktae)
                    .multiply(gram)
                    .multiply(price)
                    .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

        } else if ("roll".equalsIgnoreCase(category)
                || "reel".equalsIgnoreCase(category)) {

            return BigDecimal.valueOf(quantity)
                    .multiply(size)
                    .multiply(price);

        } else {

            return BigDecimal.valueOf(quantity)
                    .multiply(price);
        }
    }


    private void updateCompanyBill(String vendor,
                                   BigDecimal totalPrice,
                                   String status,
                                   BigDecimal oldTotal) {

        YearMonth currentMonth = YearMonth.now();

        CompanyBillAmountPaid currentMonthRecord =
                companyBillRepo.findByVendorNameAndBillingMonth(vendor, currentMonth);

        if (currentMonthRecord != null) {

            BigDecimal balance = currentMonthRecord.getBalance();

            if ("Purchase".equals(status)) {

                if (oldTotal != null) {
                    balance = balance.subtract(oldTotal);
                }
                balance = balance.add(totalPrice);

            } else {

                if (oldTotal != null) {
                    balance = balance.add(oldTotal);
                }
                balance = balance.subtract(totalPrice);
            }

            currentMonthRecord.setBalance(balance);
            companyBillRepo.save(currentMonthRecord);
            return;
        }

        // 🔹 New month → carry forward last balance
        CompanyBillAmountPaid lastMonthRecord =
                companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendor);

        BigDecimal carryForwardBalance =
                lastMonthRecord != null ? lastMonthRecord.getBalance() : BigDecimal.ZERO;

        BigDecimal finalBalance = carryForwardBalance;

        if ("Purchase".equals(status)) {

            if (oldTotal != null) {
                finalBalance = finalBalance.subtract(oldTotal);
            }
            finalBalance = finalBalance.add(totalPrice);

        } else {

            if (oldTotal != null) {
                finalBalance = finalBalance.add(oldTotal);
            }
            finalBalance = finalBalance.subtract(totalPrice);
        }

        CompanyBillAmountPaid newBill = new CompanyBillAmountPaid();
        newBill.setVendorName(vendor);
        newBill.setBillingMonth(currentMonth);
        newBill.setBalance(finalBalance);

        companyBillRepo.save(newBill);
    }


    @Transactional
    public Status updateStock(int invoiceNumber,
                              List<ProductDto> productDtos,
                              BigDecimal invoiceDiscount,
                              BigDecimal invoiceRent,
                              String description,
                              String vendorName,
                              BigDecimal payBill,
                              String status,
                              BigDecimal gstPercentage, // 🔴 NEW: GST parameter
                              BigDecimal gstAmount,     // 🔴 NEW: Optional pre-calculated GST
                              BigDecimal totalBeforeGst // 🔴 NEW: Subtotal before GST
    ) {


        if (productDtos == null || productDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

        BigDecimal invoiceTotal = BigDecimal.ZERO;

        YearMonth currentMonth= YearMonth.now();

        // 🔴 STEP 1: Remove existing products of this invoice (SOFT DELETE)
        List<ProductEntity> oldProducts =
                productRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true);

        for (ProductEntity old : oldProducts) {
            // 🔴 inventory se purani quantity minus (LOGIC SAME – sirf add)
            InventoryEntity oldInventory =
                    inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(
                            old.getCategory(), old.getProductName());

            if (oldInventory != null) {
                if ("Purchase".equals(status)) {
                    int updatedQty = oldInventory.getQuantity() - old.getQuantity();
                    oldInventory.setQuantity(updatedQty < 0 ? 0 : updatedQty);
                    oldInventory.setTotalPrice(calculateInventoryTotal(oldInventory));
                    inventoryRepo.save(oldInventory);
                } else {
                    int updatedQty = oldInventory.getQuantity() + old.getReturnedQuantity();
                    oldInventory.setQuantity(updatedQty < 0 ? 0 : updatedQty);
                    oldInventory.setTotalPrice(calculateInventoryTotal(oldInventory));
                    inventoryRepo.save(oldInventory);
                }
                old.setIsActive(false);
                old.setRecordUpdatedTime(LocalDateTime.now());
                productRepo.save(old);
            }
        }

        // 🔴 STEP 2: Save NEW products sent in request
        for (ProductDto dto : productDtos) {

            ProductEntity product = new ProductEntity();
            product.setInvoiceNumber(invoiceNumber);
            product.setCategory(dto.getCategory());
            product.setProductName(dto.getProductName());
            product.setQuantity(dto.getQuantity());
            product.setReturnedQuantity(dto.getReturnedQuantity());
            product.setPrice(dto.getPrice());
            product.setSize(dto.getSize());
            product.setKtae(dto.getKtae());
            product.setGram(dto.getGram());
            product.setIsActive(true);
            if("Purchase".equals(status)){
                product.setProductEntryTime(null);
            } else {
                product.setReturnTime(null);
            }
            product.setRecordUpdatedTime(LocalDateTime.now());
            product.setStatus(status);

            // 🔴 PRICE CALCULATION (UNCHANGED)
            BigDecimal totalPrice;
            String category = dto.getCategory();

            if ("Purchase".equals(status)) {
                if ("packet".equalsIgnoreCase(category)) {
                    totalPrice = BigDecimal.valueOf(dto.getQuantity()).multiply(dto.getSize())
                            .multiply(dto.getKtae())
                            .multiply(dto.getGram())
                            .multiply(dto.getPrice())
                            .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

                } else if ("roll".equalsIgnoreCase(category) || "reel".equalsIgnoreCase(category)) {
                    totalPrice = dto.getSize()
                            .multiply(BigDecimal.valueOf(dto.getQuantity()))
                            .multiply(dto.getPrice());

                } else {
                    totalPrice = dto.getPrice()
                            .multiply(BigDecimal.valueOf(dto.getQuantity()));
                }
                product.setTotalPrice(totalPrice);
            } else {
                if ("packet".equalsIgnoreCase(category)) {
                    totalPrice = BigDecimal.valueOf(dto.getReturnedQuantity()).multiply(dto.getSize())
                            .multiply(dto.getKtae())
                            .multiply(dto.getGram())
                            .multiply(dto.getPrice())
                            .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

                } else if ("roll".equalsIgnoreCase(category) || "reel".equalsIgnoreCase(category)) {
                    totalPrice = dto.getSize()
                            .multiply(BigDecimal.valueOf(dto.getReturnedQuantity()))
                            .multiply(dto.getPrice());

                } else {
                    totalPrice = dto.getPrice()
                            .multiply(BigDecimal.valueOf(dto.getReturnedQuantity()));
                }
                product.setTotalPrice(totalPrice);
            }

            productRepo.save(product);

            // 🔴 inventory update (SAME LOGIC)
            InventoryEntity inventory =
                    inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(
                            dto.getCategory(), dto.getProductName());

            if (inventory != null) {
                if ("Purchase".equals(status)) {
                    inventory.setQuantity(inventory.getQuantity() + dto.getQuantity());
                    inventory.setPurchasePrice(dto.getPrice());
                    inventory.setTotalPrice(calculateInventoryTotal(inventory));
                } else if (inventory.getQuantity() >= dto.getReturnedQuantity()) {
                    inventory.setQuantity(inventory.getQuantity() - dto.getReturnedQuantity());
                    inventory.setPurchasePrice(dto.getPrice());
                    inventory.setTotalPrice(calculateInventoryTotal(inventory));
                } else {
                    return new Status(StatusMessage.FAILURE, "Returned quantity is not present in your inventory");
                }
            } else {
                inventory = new InventoryEntity();
                inventory.setCategory(dto.getCategory());
                inventory.setProductName(dto.getProductName());
                inventory.setQuantity(dto.getQuantity());
                inventory.setPurchasePrice(dto.getPrice());
                inventory.setTotalPrice(totalPrice);
            }
            inventoryRepo.save(inventory);

            invoiceTotal = invoiceTotal.add(totalPrice);
        }

        // 🔴 invoice discount & rent (UNCHANGED)
        if (invoiceDiscount != null) invoiceTotal = invoiceTotal.subtract(invoiceDiscount);
        if (invoiceRent != null) invoiceTotal = invoiceTotal.add(invoiceRent);

        // 🔴 GST CALCULATION (ADDED - OPTIONAL)
        BigDecimal calculatedGstAmount = BigDecimal.ZERO;
        BigDecimal calculatedTotalBeforeGst = invoiceTotal; // Store amount before GST

        if (gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
            // If gstAmount is provided, use it, otherwise calculate
            if (gstAmount != null && gstAmount.compareTo(BigDecimal.ZERO) > 0) {
                calculatedGstAmount = gstAmount;
            } else {
                calculatedGstAmount = invoiceTotal.multiply(gstPercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            invoiceTotal = invoiceTotal.add(calculatedGstAmount);

            // If totalBeforeGst is provided, use it
            if (totalBeforeGst != null) {
                calculatedTotalBeforeGst = totalBeforeGst;
            }
        }
        // 🔴 END GST CALCULATION

        if (payBill != null) invoiceTotal = invoiceTotal.subtract(payBill);


        CompanyInvoiceAmount invoiceAmount =
                companyInvoiceAmountRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true)
                        .orElseGet(() -> {
                            CompanyInvoiceAmount cia = new CompanyInvoiceAmount();
                            cia.setInvoiceNumber(invoiceNumber);
                            cia.setIsActive(true);
                            return cia;
                        });
        BigDecimal olTotal = invoiceAmount.getGrandTotal();

        invoiceAmount.setGrandTotal(invoiceTotal);
        invoiceAmount.setDiscount(invoiceDiscount != null ? invoiceDiscount : BigDecimal.ZERO);
        invoiceAmount.setRent(invoiceRent != null ? invoiceRent : BigDecimal.ZERO);
        invoiceAmount.setDescription(description);
        invoiceAmount.setVendorName(vendorName);
        invoiceAmount.setAmountPaid(payBill);
        invoiceAmount.setStatus(status);

        invoiceAmount.setBillingMonth(currentMonth);
        invoiceAmount.setPurchaseDate(LocalDateTime.now());
        invoiceAmount.setInvoiceDate(LocalDateTime.now());

        // 🔴 SET GST FIELDS (ADDED)
        invoiceAmount.setGstPercentage(gstPercentage != null ? gstPercentage : BigDecimal.ZERO);
        invoiceAmount.setGstAmount(calculatedGstAmount);
        invoiceAmount.setTotalBeforeGst(calculatedTotalBeforeGst);

        // 🔴 END GST FIELDS

        companyInvoiceAmountRepo.save(invoiceAmount);

        // Update company bill with new total (including GST if applicable)
        updateCompanyBill(vendorName, invoiceTotal, status, olTotal);


        if (payBill != null && payBill.compareTo(BigDecimal.ZERO) > 0) {

            Optional<CompanyPaymentTime> companyPaymentTime = companyPaymentTimeRepo.findByInvoiceNumberAndIsActive(invoiceNumber,true);
            if (companyPaymentTime.isPresent()){
                CompanyPaymentTime companyPaymentTime1 = companyPaymentTime.get();
                companyPaymentTime1.setIsActive(false);
                companyPaymentTimeRepo.save(companyPaymentTime1);
            }

            // insert fresh payment record
            CompanyPaymentTime paymentTime = new CompanyPaymentTime();
            paymentTime.setInvoiceNumber(invoiceNumber);
            paymentTime.setVendorName(vendorName);
            paymentTime.setAmountPaid(payBill);
            paymentTime.setPaymentTime(LocalDateTime.now());
            paymentTime.setBillingMonth(currentMonth);
            paymentTime.setIsActive(true);

            companyPaymentTimeRepo.save(paymentTime);
        }

        return new Status(StatusMessage.SUCCESS, "Invoice stock updated successfully");
    }

    public Status searchProducts(String status, String category, String productName,
                                 LocalDate startDate, LocalDate endDate) {

        // Convert dates to LocalDateTime for proper time comparison
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate != null) {
            startDateTime = startDate.atStartOfDay();
        }

        if (endDate != null) {
            endDateTime = endDate.atTime(23, 59, 59);
        }

        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return new Status(StatusMessage.FAILURE, "Start date cannot be after end date");
        }

        // Handle productName conversion if it's a string name
        String productNameParam = productName;
        if (productName != null && !productName.trim().isEmpty()) {
            // Check if productName is a string name that needs to be converted to ID
            try {
                // Try to parse as Long first (if it's already an ID)
                Long.parseLong(productName);
                // It's already a numeric ID, use as is
                productNameParam = productName;
            } catch (NumberFormatException e) {
                // It's a string name, convert to ID
                Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName.trim());
                if (pnOpt.isEmpty()) {
                    return new Status(StatusMessage.FAILURE, "Product name not found: " + productName);
                }
                productNameParam = String.valueOf(pnOpt.get().getId());
            }
        }

        // Single query call with all filters
        List<ProductEntity> products = productRepo.findWithStatusBasedFilters(
                status,
                (category != null && !category.trim().isEmpty()) ? category.trim() : null,
                (productNameParam != null && !productNameParam.trim().isEmpty()) ? productNameParam.trim() : null,
                startDateTime,
                endDateTime
        );

        if (products == null || products.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No matching products found");
        }

        // --- Group products by invoiceNumber (same as before) ---
        Map<Integer, List<ProductDto>> invoiceMap = new HashMap<>();

        for (ProductEntity p : products) {
            ProductDto dto = new ProductDto();
            String actualProductName = p.getProductName();

            try {
                Long pid = Long.valueOf(p.getProductName());
                actualProductName = productNameRepository.findById(pid)
                        .map(ProductName::getProductName)
                        .orElse(p.getProductName());
            } catch (NumberFormatException ignored) {}

            dto.setProductName(actualProductName);
            dto.setCategory(p.getCategory());
            dto.setReturnedQuantity(p.getReturnedQuantity());
            dto.setPrice(p.getPrice());
            dto.setQuantity(p.getQuantity());
            dto.setTotalPrice(p.getTotalPrice());
            dto.setProductEntryTime(p.getProductEntryTime());
            dto.setRecordUpdatedTime(p.getRecordUpdatedTime());
            dto.setSize(p.getSize());
            dto.setKtae(p.getKtae());
            dto.setGram(p.getGram());
            dto.setIsActive(p.getIsActive());
            dto.setReturnTime(p.getReturnTime());

            int invoiceNo = p.getInvoiceNumber();
            invoiceMap.computeIfAbsent(invoiceNo, k -> new ArrayList<>()).add(dto);
        }

        // --- Convert to final InvoiceDto list ---
        List<InvoiceDto> invoiceList = invoiceMap.entrySet().stream()
                .map(entry -> {
                    int invoiceNo = entry.getKey();
                    List<ProductDto> productList = entry.getValue();

                    InvoiceDto invoiceDto = new InvoiceDto();
                    invoiceDto.setInvoiceNumber(invoiceNo);
                    invoiceDto.setProducts(productList);

                    // --- Fetch invoice-level details from company_invoice_amount ---
                    Optional<CompanyInvoiceAmount> ciaOpt = companyInvoiceAmountRepo
                            .findByInvoiceNumberAndStatusAndIsActive(invoiceNo, status, true);

                    if (ciaOpt.isPresent()) {
                        CompanyInvoiceAmount cia = ciaOpt.get();
                        invoiceDto.setVendorName(cia.getVendorName());
                        invoiceDto.setInvoiceDiscount(cia.getDiscount());
                        invoiceDto.setInvoiceRent(cia.getRent());
                        invoiceDto.setAmountPaid(cia.getAmountPaid());
                        invoiceDto.setDescription(cia.getDescription());
                        invoiceDto.setGrandTotal(cia.getGrandTotal());

                        // 🔴 ADD GST FIELDS - ADDED
                        invoiceDto.setGstPercentage(cia.getGstPercentage());
                        invoiceDto.setGstAmount(cia.getGstAmount());
                        invoiceDto.setTotalBeforeGst(cia.getTotalBeforeGst());
                        // 🔴 END GST FIELDS

                        if (cia.getInvoiceImagePath() != null && !cia.getInvoiceImagePath().isBlank()) {
                            String baseUrl = "http://localhost:8081/pos/product/invoice-image?invoiceImagePath=";
                            invoiceDto.setInvoiceImagePath(baseUrl + URLEncoder.encode(
                                    cia.getInvoiceImagePath(), StandardCharsets.UTF_8));
                        }
                    }

                    return invoiceDto;
                }).collect(Collectors.toList());

        return new Status(StatusMessage.SUCCESS, invoiceList);
    }


    public Status deleteRecord(Integer invoiceNumber, String status) {
        List<ProductEntity> products = productRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true);
        if (products == null || products.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Record is already deleted");
        }

        Optional<CompanyInvoiceAmount> companyInvoice = companyInvoiceAmountRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true);
        if (companyInvoice.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No record found against this invoice number");
        }

        for (ProductEntity product : products) {
            Integer oldQuantity = product.getQuantity();
            Integer oldReturnedQuantity = product.getReturnedQuantity();
            BigDecimal oldTotalPrice = companyInvoice.get().getGrandTotal();

            if ("Purchase".equals(status)) {

                InventoryEntity inventory = inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(product.getCategory(), product.getProductName());

                if (inventory != null) {
                    Integer newQuantity = inventory.getQuantity() - oldQuantity;
                    inventory.setQuantity(newQuantity);
                    inventory.setTotalPrice(calculateInventoryTotal(inventory));
                    inventoryRepo.save(inventory);
                }
            } else {
                InventoryEntity inventory = inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(product.getCategory(), product.getProductName());
                if (inventory != null) {
                    Integer newQuantity = inventory.getQuantity() + oldReturnedQuantity;
                    inventory.setQuantity(newQuantity);
                    inventory.setTotalPrice(calculateInventoryTotal(inventory));
                    inventoryRepo.save(inventory);
                }
            }


            if ("Purchase".equals(status)) {
                CompanyBillAmountPaid companyBillAmountPaid = companyBillAmountPaidRepo.findByVendorName(companyInvoice.get().getVendorName());
                if (companyBillAmountPaid != null) {
                    BigDecimal newBalance = companyBillAmountPaid.getBalance().subtract(oldTotalPrice);
                    companyBillAmountPaid.setBalance(newBalance);
                    companyBillAmountPaidRepo.save(companyBillAmountPaid);
                }
            } else {
                CompanyBillAmountPaid companyBillAmountPaid = companyBillAmountPaidRepo.findByVendorName(companyInvoice.get().getVendorName());
                if (companyBillAmountPaid != null) {
                    BigDecimal newBalance = companyBillAmountPaid.getBalance().add(oldTotalPrice);
                    companyBillAmountPaid.setBalance(newBalance);
                    companyBillAmountPaidRepo.save(companyBillAmountPaid);
                }
            }

            product.setIsActive(false);
            product.setRecordDeletedTime(LocalDateTime.now());
            productRepo.save(product);
        }
        Optional<CompanyInvoiceAmount> companyInvoiceAmountOpt = companyInvoiceAmountRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true);

        if (companyInvoiceAmountOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Record already deleted");
        }

        CompanyInvoiceAmount companyInvoiceAmount = companyInvoiceAmountOpt.get();
        companyInvoiceAmount.setIsActive(false);
        companyInvoiceAmountRepo.save(companyInvoiceAmount);

        Optional<CompanyPaymentTime> companyPaymentTime=companyPaymentTimeRepo.findByInvoiceNumberAndIsActive(invoiceNumber,true);

        if (companyPaymentTime.isPresent()){
            CompanyPaymentTime companyPaymentTime1= companyPaymentTime.get();
            companyPaymentTime1.setIsActive(false);
            companyPaymentTimeRepo.save(companyPaymentTime1);
        }

        return new Status(StatusMessage.SUCCESS, "Record is deleted");

    }

//    public Status payVendorBill(String vendorName, BigDecimal amount) {
//        CompanyBillAmountPaid bill = companyBillRepo.findByVendorName(vendorName);
//        if (bill == null) {
//            return new Status(StatusMessage.FAILURE, "No bill record found for vendor: " + vendorName);
//        }
//
//        BigDecimal newBalance = bill.getBalance().subtract(amount);
//        BigDecimal billPaid=bill.getBillPaid().add(amount);
//        bill.setBillPaid(billPaid);
//        bill.setBalance(newBalance);
//
//        companyBillRepo.save(bill);
//
//        CompanyPaymentTime paymentTime = new CompanyPaymentTime();
//        paymentTime.setVendorName(vendorName);
//        paymentTime.setAmountPaid(amount);
//        paymentTime.setPaymentTime(LocalDateTime.now());
//        companyPaymentTimeRepo.save(paymentTime);
//
//        return new Status(StatusMessage.SUCCESS, "Payment successful. Updated balance: " + newBalance);
//    }

    public Status getAllProductNamesAndPrices() {

        List<ProductName> products = productNameRepository.findByIsActive(true);

        List<ProductNameResponseDto> dtoList = products.stream()
                .map(product -> {

                    ProductNameResponseDto dto = new ProductNameResponseDto();
                    dto.setProductName(product.getProductName());
                    dto.setPurchasePrice(product.getPurchasePrice());
                    dto.setSellPrice(product.getSellPrice());

                    // Fetch category by product.getCategoryId()
                    Optional<Category> cat = categoryRepository.findById(product.getCategory().getId());

                    if (cat.isPresent()) {
                        dto.setCategory(cat.get().getCategoryName()); // category name
                    } else {
                        dto.setCategory("Unknown"); // fallback
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return new Status(StatusMessage.SUCCESS, dtoList);
    }

    @Transactional
    public Status revertStock(
            int invoiceNumber,
            String vendorName,
            BigDecimal invoiceDiscount,
            BigDecimal invoiceRent,
            String description,
            List<RevertDto> revertDtos,
            BigDecimal gstPercentage) { // 🔴 NEW: Add GST parameter

        if (revertDtos == null || revertDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE,
                    "Revert data is missing");
        }

        YearMonth currentMonth = YearMonth.now();
        BigDecimal totalRevertAmount = BigDecimal.ZERO;

    /* =====================================================
       STEP 1: PROCESS EACH REVERT ITEM
       ===================================================== */
        for (RevertDto dto : revertDtos) {

            // 1️⃣ Inventory check
            InventoryEntity inventory =
                    inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(
                            dto.getCategory(),
                            dto.getProductName()
                    );

            if (inventory == null) {
                return new Status(StatusMessage.FAILURE,
                        "Inventory not found for product: " + dto.getProductName());
            }

            if (inventory.getQuantity() < dto.getQuantity()) {
                return new Status(StatusMessage.FAILURE,
                        "Insufficient stock for product: " + dto.getProductName());
            }

            // 2️⃣ Minus quantity from inventory
            inventory.setQuantity(
                    inventory.getQuantity() - dto.getQuantity()
            );

            inventory.setTotalPrice(
                    calculateInventoryTotal(inventory));

            inventoryRepo.save(inventory);

            // 4️⃣ Save revert history (per product)
            ProductEntity revert = new ProductEntity();
            revert.setInvoiceNumber(invoiceNumber);
            revert.setProductName(dto.getProductName());
            revert.setCategory(dto.getCategory());
            revert.setReturnedQuantity(dto.getQuantity());
            revert.setPrice(dto.getPrice());
            revert.setStatus("Return");
            revert.setReturnTime(LocalDateTime.now());
            revert.setIsActive(true);

            BigDecimal totalPrice;
            if ("packet".equalsIgnoreCase(dto.getCategory())) {
                Integer quantity = dto.getQuantity();
                BigDecimal size = dto.getSize() != null ? dto.getSize() : BigDecimal.ONE;
                BigDecimal ktae = dto.getKtae() != null ? dto.getKtae() : BigDecimal.ONE;
                BigDecimal gram = dto.getGram() != null ? dto.getGram() : BigDecimal.ONE;
                totalPrice = BigDecimal.valueOf(quantity)
                        .multiply(size)
                        .multiply(ktae)
                        .multiply(gram)
                        .multiply(dto.getPrice())
                        .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

                revert.setSize(size);
                revert.setKtae(ktae);
                revert.setGram(gram);
            } else if ("roll".equalsIgnoreCase(dto.getCategory()) || ("reel".equalsIgnoreCase(dto.getCategory()))) {
                BigDecimal size = dto.getSize() != null ? dto.getSize() : BigDecimal.ONE;
                totalPrice = size.multiply(BigDecimal.valueOf(dto.getQuantity())).multiply(dto.getPrice());
                revert.setSize(size);
            } else {
                totalPrice = dto.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
            }

            revert.setTotalPrice(totalPrice);

            productRepo.save(revert);
            totalRevertAmount = totalRevertAmount.add(totalPrice);
        }

        // Apply discount and rent
        if (invoiceDiscount != null) totalRevertAmount = totalRevertAmount.subtract(invoiceDiscount);
        if (invoiceRent != null) totalRevertAmount = totalRevertAmount.add(invoiceRent);

        // 🔴 GST CALCULATION (ADDED - OPTIONAL)
        BigDecimal gstAmount = BigDecimal.ZERO;
        BigDecimal totalBeforeGst = totalRevertAmount; // Store amount before GST

        if (gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
            gstAmount = totalRevertAmount.multiply(gstPercentage)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            totalRevertAmount = totalRevertAmount.add(gstAmount);
        }
        // 🔴 END GST CALCULATION

        // --- Save invoice summary in CompanyInvoiceAmount ---
        CompanyInvoiceAmount invoiceAmount = new CompanyInvoiceAmount();
        invoiceAmount.setInvoiceNumber(invoiceNumber);
        invoiceAmount.setGrandTotal(totalRevertAmount);
        invoiceAmount.setDiscount(invoiceDiscount != null ? invoiceDiscount : BigDecimal.ZERO);
        invoiceAmount.setRent(invoiceRent != null ? invoiceRent : BigDecimal.ZERO);
        invoiceAmount.setDescription(description);
        invoiceAmount.setVendorName(vendorName);
        invoiceAmount.setStatus("Return");
        invoiceAmount.setIsActive(true);

        // 🔴 SET GST FIELDS (ADDED)
        invoiceAmount.setGstPercentage(gstPercentage != null ? gstPercentage : BigDecimal.ZERO);
        invoiceAmount.setGstAmount(gstAmount);
        invoiceAmount.setTotalBeforeGst(totalBeforeGst);

        invoiceAmount.setBillingMonth(currentMonth);
        invoiceAmount.setPurchaseDate(LocalDateTime.now());
        invoiceAmount.setInvoiceDate(LocalDateTime.now());
        // 🔴 END GST FIELDS

        companyInvoiceAmountRepo.save(invoiceAmount);

    /* =====================================================
       STEP 2: UPDATE COMPANY BILL (ONCE)
       ===================================================== */
        CompanyBillAmountPaid bill =
                companyBillRepo.findByVendorNameAndBillingMonth(
                        vendorName, currentMonth
                );

        if (bill == null) {
            return new Status(StatusMessage.FAILURE,
                    "Vendor bill not found for current month");
        }

        // Update balance with final amount (including GST if applicable)
        bill.setBalance(
                bill.getBalance().subtract(totalRevertAmount)
        );

        companyBillRepo.save(bill);

        return new Status(
                StatusMessage.SUCCESS,
                "Stock reverted successfully"
        );
    }


//    public BigDecimal getCompanyBalanceForMonth(int year, int month) {
//        YearMonth start = YearMonth.of(year, month);
//        return companyBillAmountPaidRepo.getCompanyBalanceByMonth(start);
//    }

}

