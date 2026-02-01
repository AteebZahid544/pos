package com.example.pos.Service;

import com.example.pos.DTO.*;
import com.example.pos.entity.pos.*;
import com.example.pos.repo.pos.*;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Month;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesService {
    @Autowired
    private SalesRepo salesRepo;

    @Autowired
    private InventoryRepo inventoryRepo;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductNameRepository productNameRepository;

    @Autowired
    private CustomerInvoiceRecordRepo customerInvoiceRecordRepo;

    @Autowired
    private CustomerPaymentTimeRepo customerPaymentTimeRepo;

    @Autowired
    private CustomerBillAmountPaidRepo customerBillRepo;

    @Value("${invoice.upload.path}")
    private String invoiceUploadPath;

    public Status productSold(List<SalesDto> productDtos,
                              BigDecimal invoiceDiscount,
                              BigDecimal invoiceRent,
                              String description,
                              String customerName,
                              BigDecimal payBill,
                              String status,
                              BigDecimal gstPercentage) {

        YearMonth currentMonth= YearMonth.now();

        if (productDtos == null || productDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

        BigDecimal invoiceTotal = BigDecimal.ZERO;
        BigDecimal totalPurchaseCost = BigDecimal.ZERO; // ✅ Track purchase cost

        int invoiceNumber = productDtos.get(0).getInvoiceNumber();

        // --- Save sold products & calculate invoice total ---
        for (SalesDto productDto : productDtos) {

            Optional<ProductName> productNameOpt =
                    productNameRepository.findByProductNameAndIsActive(productDto.getProductName(), true);

            if (productNameOpt.isEmpty()) {
                return new Status(StatusMessage.FAILURE,
                        "Product Name : " + productDto.getProductName() + " not found");
            }

            ProductName productName = productNameOpt.get();
            Category category = productName.getCategory();

            if (category == null) {
                return new Status(StatusMessage.FAILURE,
                        "Category not found for product: " + productDto.getProductName());
            }
            BigDecimal purchasePrice = productName.getPurchasePrice();


            // 🔴 INVENTORY CHECK
            InventoryEntity inventory =
                    inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(
                            category.getCategoryName(),
                            productDto.getProductName());

            if ("Sale".equals(status)) {
                if (inventory == null || inventory.getQuantity() < productDto.getQuantity()) {
                    return new Status(StatusMessage.FAILURE,
                            "Insufficient stock for product: " + productDto.getProductName());
                }
            } if (inventory == null) {
                inventory = new InventoryEntity(); // 🔴 IMPORTANT: assign to inventory
                inventory.setProductName(productDto.getProductName());
                inventory.setCategory(category.getCategoryName());
                inventory.setPurchasePrice(productDto.getPrice());
                inventory.setQuantity(0);
                inventory.setSize(productDto.getSize());
                inventory.setKtae(productDto.getKtae());
                inventory.setGram(productDto.getGram());

            }

            // --- Create sold product record ---
            SalesEntity product = new SalesEntity();
            product.setProductName(productDto.getProductName());
            product.setCategory(category.getCategoryName());
            product.setPrice(productDto.getPrice());
            product.setPurchasePrice(purchasePrice); // ✅ Save purchase price

            product.setInvoiceNumber(invoiceNumber);
            if ("Sale".equals(status)) {
                product.setQuantity(productDto.getQuantity());
                product.setSaleEntryTime(LocalDateTime.now());
            } else {
                product.setReturnedQuantity(productDto.getReturnedQuantity());
                product.setReturnTime(LocalDateTime.now());
            }
            product.setIsActive(true);

            BigDecimal purchaseCost = BigDecimal.ZERO;
            BigDecimal totalPrice ;

            if("Sale".equals(status)){

                Integer quantity = productDto.getQuantity();

                if ("packet".equalsIgnoreCase(category.getCategoryName())) {

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

                    purchaseCost = BigDecimal.valueOf(quantity)
                            .multiply(size)
                            .multiply(ktae)
                            .multiply(gram)
                            .multiply(purchasePrice)
                            .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

                    product.setSize(size);
                    product.setKtae(ktae);
                    product.setGram(gram);

                } else if ("roll".equalsIgnoreCase(category.getCategoryName())
                        || "reel".equalsIgnoreCase(category.getCategoryName())) {

                    BigDecimal size = productDto.getSize() != null ? productDto.getSize() : BigDecimal.ONE;
                    totalPrice = size
                            .multiply(BigDecimal.valueOf(quantity))
                            .multiply(productDto.getPrice());

                    purchaseCost = size
                            .multiply(BigDecimal.valueOf(quantity))
                            .multiply(purchasePrice);

                    product.setSize(size);

                } else {
                    totalPrice = productDto.getPrice()
                            .multiply(BigDecimal.valueOf(productDto.getQuantity()));

                    purchaseCost = purchasePrice
                            .multiply(BigDecimal.valueOf(quantity));
                }
                product.setTotalPrice(totalPrice);}
            else {
                Integer returnedQuantity = productDto.getReturnedQuantity();
                if ("packet".equalsIgnoreCase(category.getCategoryName())) {

                    BigDecimal size = productDto.getSize() != null ? productDto.getSize() : BigDecimal.ONE;
                    BigDecimal ktae = productDto.getKtae() != null ? productDto.getKtae() : BigDecimal.ONE;
                    BigDecimal gram = productDto.getGram() != null ? productDto.getGram() : BigDecimal.ONE;

                    totalPrice = BigDecimal.valueOf(returnedQuantity)
                            .multiply(size)
                            .multiply(ktae)
                            .multiply(gram)
                            .multiply(productDto.getPrice())
                            .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

                    purchaseCost = BigDecimal.valueOf(returnedQuantity)
                            .multiply(size)
                            .multiply(ktae)
                            .multiply(gram)
                            .multiply(purchasePrice)
                            .divide(BigDecimal.valueOf(15500), 2, BigDecimal.ROUND_HALF_UP);

                    product.setSize(size);
                    product.setKtae(ktae);
                    product.setGram(gram);

                } else if ("roll".equalsIgnoreCase(category.getCategoryName())
                        || "reel".equalsIgnoreCase(category.getCategoryName())) {

                    BigDecimal size = productDto.getSize() != null ? productDto.getSize() : BigDecimal.ONE;
                    totalPrice = size
                            .multiply(BigDecimal.valueOf(returnedQuantity))
                            .multiply(productDto.getPrice());

                    purchaseCost = size
                            .multiply(BigDecimal.valueOf(returnedQuantity))
                            .multiply(purchasePrice);

                    product.setSize(size);

                } else {
                    totalPrice = productDto.getPrice()
                            .multiply(BigDecimal.valueOf(returnedQuantity));

                    purchaseCost = purchasePrice
                            .multiply(BigDecimal.valueOf(returnedQuantity));

                    // For returns, make purchase cost negative
                    purchaseCost = purchaseCost.negate();
                }
                product.setTotalPrice(totalPrice);
            }
            product.setPurchaseCost(purchaseCost);

            product.setStatus(status);
            salesRepo.save(product);

            if ("Sale".equals(status)){
                inventory.setQuantity(inventory.getQuantity() - productDto.getQuantity());}
            else {
                inventory.setQuantity(inventory.getQuantity() + productDto.getReturnedQuantity());
            }
            inventory.setTotalPrice(calculateInventoryTotal(inventory));

            inventoryRepo.save(inventory);

            invoiceTotal = invoiceTotal.add(totalPrice);
            totalPurchaseCost = totalPurchaseCost.add(purchaseCost);

        }

        // --- Invoice discount & rent ---
        if (invoiceDiscount != null) invoiceTotal = invoiceTotal.subtract(invoiceDiscount);
        if (invoiceRent != null) invoiceTotal = invoiceTotal.add(invoiceRent);

        BigDecimal gstAmount = BigDecimal.ZERO;

        // 🔴 GST CALCULATION (ADDED - OPTIONAL)
        if (gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
             gstAmount = invoiceTotal.multiply(gstPercentage)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            invoiceTotal = invoiceTotal.add(gstAmount);
        }
        // 🔴 END OF GST ADDITION

        if (payBill != null) invoiceTotal = invoiceTotal.subtract(payBill);


        // --- Save invoice summary ---
        CustomerInvoiceRecord invoiceAmount = new CustomerInvoiceRecord();
        invoiceAmount.setInvoiceNumber(invoiceNumber);
        invoiceAmount.setGrandTotal(invoiceTotal);
        invoiceAmount.setDiscount(invoiceDiscount != null ? invoiceDiscount : BigDecimal.ZERO);
        invoiceAmount.setRent(invoiceRent != null ? invoiceRent : BigDecimal.ZERO);
        invoiceAmount.setDescription(description);
        invoiceAmount.setCustomerName(customerName);
        invoiceAmount.setBillingMonth(YearMonth.now());
        invoiceAmount.setIsActive(true);
        invoiceAmount.setStatus(status);
        invoiceAmount.setBillingMonth(currentMonth);
        invoiceAmount.setSaleDate(LocalDateTime.now());
        invoiceAmount.setInvoiceDate(LocalDateTime.now());
        invoiceAmount.setTotalPurchaseCost(totalPurchaseCost);

        if (payBill != null) {
            invoiceAmount.setAmountPaid(payBill);
        }
        if (gstPercentage!=null){
            invoiceAmount.setGstPercentage(gstPercentage);
            invoiceAmount.setGstAmount(gstAmount);
            invoiceAmount.setTotalBeforeGst(invoiceTotal.subtract(gstAmount));
        }

        customerInvoiceRecordRepo.save(invoiceAmount);

        updateCustomerBill(customerName, invoiceTotal, status, null);



        if ("Sale".equals(status)){
            CustomerPaymentTime payment = new CustomerPaymentTime();
            payment.setInvoiceNumber(invoiceNumber);
            payment.setCustomerName(customerName);
            payment.setAmountPaid(payBill);
            payment.setPaymentTime(LocalDateTime.now());
            payment.setBillingMonth(currentMonth);
            payment.setIsActive(true);

            customerPaymentTimeRepo.save(payment);}
        // --- Handle payment ---
//        if (payBill != null && payBill.compareTo(BigDecimal.ZERO) > 0) {
//            handleCustomerPayment(customerName, payBill, invoiceNumber);
//        }

        return new Status(StatusMessage.SUCCESS, "Products sold successfully");
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
        CustomerInvoiceRecord invoice = customerInvoiceRecordRepo
                .findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setInvoiceImagePath(destinationFile.getAbsolutePath());
        customerInvoiceRecordRepo.save(invoice);
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


    private void updateCustomerBill(String customer,
                                   BigDecimal totalPrice,
                                   String status,
                                   BigDecimal oldTotal) {

        YearMonth currentMonth = YearMonth.now();

        CustomerBillAmountPaid currentMonthRecord =
                customerBillRepo.findByCustomerNameAndBillingMonth(customer, currentMonth);

        if (currentMonthRecord != null) {

            BigDecimal balance = currentMonthRecord.getBalance();

            if ("Sale".equals(status)) {

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
            customerBillRepo.save(currentMonthRecord);
            return;
        }

        // 🔹 New month → carry forward last balance
        CustomerBillAmountPaid lastMonthRecord =
                customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(customer);

        BigDecimal carryForwardBalance =
                lastMonthRecord != null ? lastMonthRecord.getBalance() : BigDecimal.ZERO;

        BigDecimal finalBalance = carryForwardBalance;

        if ("Sale".equals(status)) {

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

        CustomerBillAmountPaid newBill = new CustomerBillAmountPaid();
        newBill.setCustomerName(customer);
        newBill.setBillingMonth(currentMonth);
        newBill.setBalance(finalBalance);

        customerBillRepo.save(newBill);
    }
//    private void handleCustomerPayment(String customerName,
//                                       BigDecimal payBill,
//                                       int invoiceNumber) {
//        YearMonth currentMonth = YearMonth.now();
//
//        // --- Find current month bill ---
//        CustomerBillAmountPaid bill =
//                customerBillRepo.findByCustomerNameAndBillingMonth(customerName, currentMonth);
//
//        if (bill == null) {
//            // fallback: last month
//            bill = customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(customerName);
//
//            if (bill == null) {
//                throw new RuntimeException("No bill found for customer: " + customerName);
//            }
//        }
//
//        // --- Minus payment from balance ---
//        BigDecimal updatedBalance = bill.getBalance().subtract(payBill);
//
//        bill.setBalance(updatedBalance);
//        customerBillRepo.save(bill);
//
//        // --- Save payment history ---
//        CustomerPaymentTime payment = new CustomerPaymentTime();
//        payment.setInvoiceNumber(invoiceNumber);
//        payment.setCustomerName(customerName);
//        payment.setAmountPaid(payBill);
//        payment.setPaymentTime(LocalDateTime.now());
//        payment.setBillingMonth(currentMonth);
//
//        customerPaymentTimeRepo.save(payment);
//    }

    public Status searchProducts(String category, String productName, String status,
                                 LocalDate startDate, LocalDate endDate) {

        boolean categoryEmpty = (category == null || category.isBlank());
        boolean productEmpty = (productName == null || productName.isBlank());

        // Convert LocalDate to LocalDateTime for the query
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        List<SalesEntity> products = new ArrayList<>();

        try {
            // If productName is provided and not empty, get the product ID
            String pnIdStr = null;
            if (!productEmpty) {
                Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName);
                if (pnOpt.isEmpty()) {
                    return new Status(StatusMessage.FAILURE, "Product name not found");
                }
                pnIdStr = String.valueOf(pnOpt.get().getId());
            }

            // Use the query with date filters
            products = salesRepo.findWithStatusBasedFilters(
                    status,
                    categoryEmpty ? null : category,
                    productEmpty ? null : pnIdStr,
                    startDateTime,
                    endDateTime
            );

            if (products == null || products.isEmpty()) {
                return new Status(StatusMessage.FAILURE, "No matching products found");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Status(StatusMessage.FAILURE, "Error searching products: " + e.getMessage());
        }

        // --- Group products by invoiceNumber ---
        Map<Integer, List<SalesDto>> invoiceMap = new HashMap<>();

        for (SalesEntity s : products) {
            SalesDto dto = new SalesDto();
            String actualProductName = s.getProductName();

            try {
                Long pid = Long.valueOf(s.getProductName());
                actualProductName = productNameRepository.findById(pid)
                        .map(ProductName::getProductName)
                        .orElse(s.getProductName());
            } catch (NumberFormatException ignored) {
            }

            dto.setProductName(actualProductName);
            dto.setCategory(s.getCategory());
            dto.setPrice(s.getPrice());
            dto.setQuantity(s.getQuantity());
            dto.setTotalPrice(s.getTotalPrice());
            dto.setSaleEntryTime(s.getSaleEntryTime());
            dto.setRecordUpdatedTime(s.getRecordUpdatedTime());
            dto.setSize(s.getSize());
            dto.setKtae(s.getKtae());
            dto.setGram(s.getGram());
            dto.setIsActive(s.getIsActive());
            dto.setReturnedQuantity(s.getReturnedQuantity());
            dto.setReturnTime(s.getReturnTime());

            int invoiceNo = s.getInvoiceNumber();
            invoiceMap.computeIfAbsent(invoiceNo, k -> new ArrayList<>()).add(dto);
        }

        // --- Convert to final InvoiceDto list ---
        List<InvoiceDto> invoiceList = invoiceMap.entrySet().stream()
                .map(entry -> {
                    int invoiceNo = entry.getKey();
                    List<SalesDto> productList = entry.getValue();

                    InvoiceDto invoiceDto = new InvoiceDto();
                    invoiceDto.setInvoiceNumber(invoiceNo);
                    invoiceDto.setSales(productList);

                    Optional<CustomerInvoiceRecord> ciaOpt = customerInvoiceRecordRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNo, status, true);

                    if (ciaOpt.isPresent()) {
                        CustomerInvoiceRecord cia = ciaOpt.get();
                        invoiceDto.setCustomerName(cia.getCustomerName());
                        invoiceDto.setInvoiceDiscount(cia.getDiscount());
                        invoiceDto.setInvoiceRent(cia.getRent());
                        invoiceDto.setAmountPaid(cia.getAmountPaid());
                        invoiceDto.setDescription(cia.getDescription());
                        invoiceDto.setGrandTotal(cia.getGrandTotal());
                        invoiceDto.setGstPercentage(cia.getGstPercentage());
                        invoiceDto.setGstAmount(cia.getGstAmount());
                        invoiceDto.setTotalBeforeGst(cia.getTotalBeforeGst());

                        if (cia.getInvoiceImagePath() != null && !cia.getInvoiceImagePath().isBlank()) {
                            String baseUrl = "http://localhost:8081/pos/product/sale/invoice-image?invoiceImagePath=";
                            invoiceDto.setInvoiceImagePath(baseUrl + URLEncoder.encode(cia.getInvoiceImagePath(), StandardCharsets.UTF_8));
                        }
                    }

                    return invoiceDto;
                }).collect(Collectors.toList());

        return new Status(StatusMessage.SUCCESS, invoiceList);
    }

//    @Transactional
//    public Status cancelProductSale(int id) {
//        Optional<SalesEntity> optionalSale = salesRepo.findById(id);
//        if (optionalSale.isEmpty()) {
//            return new Status(StatusMessage.FAILURE, "Sale record not found");
//        }
//
//        SalesEntity sale = optionalSale.get();
//        InventoryEntity products = inventoryRepo.findByCategoryAndProductName(sale.getCategory(), sale.getProductName());
//
//        if (products == null) {
//            return new Status(StatusMessage.FAILURE, "Product not found");
//        }
//
//        Integer restoredQuantity = products.getQuantity() + sale.getQuantity();
//        products.setQuantity(restoredQuantity);
//        inventoryRepo.save(products);
//
//        salesRepo.deleteById(id);
//
//        return new Status(StatusMessage.SUCCESS, "Sale cancelled and stock restored successfully");
//    }

    @Transactional
    public Status updateSaleEntry(int invoiceNumber,
                                  List<SalesDto> productDtos,
                                  BigDecimal invoiceDiscount,
                                  BigDecimal invoiceRent,
                                  String description,
                                  String customerName,
                                  BigDecimal amountPaid,
                                  String status,
                                  BigDecimal gstPercentage,
                                  BigDecimal gstAmount,
                                  BigDecimal totalBeforeGst) {

        if (productDtos == null || productDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

        YearMonth currentMonth = YearMonth.now();

    /* =====================================================
       STEP 1: FETCH OLD INVOICE + OLD SALES
       ===================================================== */
        CustomerInvoiceRecord oldInvoice =
                customerInvoiceRecordRepo
                        .findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));
        BigDecimal oldGrandTotal = oldInvoice.getGrandTotal();

        List<SalesEntity> oldSales =
                salesRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true);

        YearMonth billingMonth =
                oldInvoice.getBillingMonth() != null
                        ? oldInvoice.getBillingMonth()
                        : YearMonth.now();

    /* =====================================================
       STEP 2: UNDO OLD SALES (INVENTORY ADD BACK)
       ===================================================== */
        for (SalesEntity old : oldSales) {

            InventoryEntity inventory =
                    inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(
                            old.getCategory(), old.getProductName());


            if (inventory != null) {

                int undoQty =
                        "Return".equalsIgnoreCase(status)
                                ? (old.getReturnedQuantity() != null ? old.getReturnedQuantity() : 0)
                                : (old.getQuantity() != null ? old.getQuantity() : 0);

                if ("Return".equalsIgnoreCase(status)) {
                    inventory.setQuantity(inventory.getQuantity() - undoQty);
                } else {
                    inventory.setQuantity(inventory.getQuantity() + undoQty);
                }

                inventory.setTotalPrice(calculateInventoryTotal(inventory));
                inventoryRepo.save(inventory);
            }

            old.setIsActive(false);
            old.setRecordUpdatedTime(LocalDateTime.now());
            salesRepo.save(old);
        }

        oldInvoice.setIsActive(false);
        customerInvoiceRecordRepo.save(oldInvoice);

    /* =====================================================
       STEP 4: APPLY NEW SALES (INVENTORY MINUS)
       ===================================================== */
        BigDecimal newInvoiceTotal = BigDecimal.ZERO;
        BigDecimal newProductsTotal = BigDecimal.ZERO;
        BigDecimal totalPurchaseCost = BigDecimal.ZERO; // ✅ NEW: Track total purchase cost

        for (SalesDto dto : productDtos) {

            // Get product purchase price from ProductName table
            Optional<ProductName> productNameOpt = productNameRepository
                    .findByProductNameAndIsActive(dto.getProductName(), true);

            BigDecimal purchasePrice = BigDecimal.ZERO;
            if (productNameOpt.isPresent()) {
                purchasePrice = productNameOpt.get().getPurchasePrice();
            }

            SalesEntity sale = new SalesEntity();
            sale.setInvoiceNumber(invoiceNumber);
            sale.setCategory(dto.getCategory());
            sale.setProductName(dto.getProductName());
            sale.setPrice(dto.getPrice());
            sale.setPurchasePrice(purchasePrice); // ✅ NEW: Set purchase price
            sale.setSize(dto.getSize());
            sale.setKtae(dto.getKtae());
            sale.setGram(dto.getGram());
            sale.setIsActive(true);
            sale.setRecordUpdatedTime(LocalDateTime.now());

            if ("Return".equalsIgnoreCase(status)) {
                sale.setReturnedQuantity(dto.getReturnedQuantity());
            } else {
                sale.setQuantity(dto.getQuantity());
            }

            sale.setStatus(status);

            /* ---------- PRICE CALCULATION ---------- */
            BigDecimal totalPrice;
            BigDecimal purchaseCost = BigDecimal.ZERO; // ✅ NEW: Purchase cost calculation
            String category = dto.getCategory().toLowerCase();

            int qty =
                    "Return".equalsIgnoreCase(status)
                            ? (dto.getReturnedQuantity() != null ? dto.getReturnedQuantity() : 0)
                            : (dto.getQuantity() != null ? dto.getQuantity() : 0);

            if ("packet".equals(category)) {
                // Sale price calculation
                totalPrice = BigDecimal.valueOf(qty)
                        .multiply(dto.getSize())
                        .multiply(dto.getKtae())
                        .multiply(dto.getGram())
                        .multiply(dto.getPrice())
                        .divide(BigDecimal.valueOf(15500), 2, RoundingMode.HALF_UP);

                // Purchase cost calculation
                purchaseCost = BigDecimal.valueOf(qty)
                        .multiply(dto.getSize())
                        .multiply(dto.getKtae())
                        .multiply(dto.getGram())
                        .multiply(purchasePrice)
                        .divide(BigDecimal.valueOf(15500), 2, RoundingMode.HALF_UP);

            } else if ("roll".equals(category) || "reel".equals(category)) {
                // Sale price calculation
                totalPrice = dto.getSize()
                        .multiply(BigDecimal.valueOf(qty))
                        .multiply(dto.getPrice());

                // Purchase cost calculation
                purchaseCost = dto.getSize()
                        .multiply(BigDecimal.valueOf(qty))
                        .multiply(purchasePrice);

            } else {
                // Sale price calculation
                totalPrice = dto.getPrice()
                        .multiply(BigDecimal.valueOf(qty));

                // Purchase cost calculation
                purchaseCost = purchasePrice
                        .multiply(BigDecimal.valueOf(qty));
            }

            if (dto.getGeneralDiscount() != null) {
                totalPrice = totalPrice.subtract(dto.getGeneralDiscount());
            }

            sale.setTotalPrice(totalPrice);
            sale.setPurchaseCost(purchaseCost); // ✅ NEW: Set purchase cost
            salesRepo.save(sale);

            // Add to total purchase cost (negative for returns)
            if ("Return".equalsIgnoreCase(status)) {
                totalPurchaseCost = totalPurchaseCost.subtract(purchaseCost);
            } else {
                totalPurchaseCost = totalPurchaseCost.add(purchaseCost);
            }

            InventoryEntity inventory =
                    inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(
                            dto.getCategory(), dto.getProductName());

            if (inventory == null) {
                throw new RuntimeException(
                        "Inventory not found for product: " + dto.getProductName());
            }

            int applyQty =
                    "Return".equalsIgnoreCase(status)
                            ? (dto.getReturnedQuantity() != null ? dto.getReturnedQuantity() : 0)
                            : (dto.getQuantity() != null ? dto.getQuantity() : 0);

            if ("Return".equalsIgnoreCase(status)) {
                inventory.setQuantity(inventory.getQuantity() + applyQty);
            } else {
                inventory.setQuantity(inventory.getQuantity() - applyQty);
            }

            inventory.setTotalPrice(calculateInventoryTotal(inventory));
            inventoryRepo.save(inventory);

            newProductsTotal = newProductsTotal.add(totalPrice);
        }

    /* =====================================================
       STEP 5: CALCULATE INVOICE TOTAL PROPERLY
       ===================================================== */
        // Start with products total
        BigDecimal calculatedTotal = newProductsTotal;

        // Apply invoice discount and rent
        if (invoiceDiscount != null) {
            calculatedTotal = calculatedTotal.subtract(invoiceDiscount);
        }

        if (invoiceRent != null) {
            calculatedTotal = calculatedTotal.add(invoiceRent);
        }

        // ✅ Store total before GST
        BigDecimal totalBeforeGstCalculated = calculatedTotal;

        // 🔴 GST CALCULATION
        BigDecimal calculatedGstAmount = BigDecimal.ZERO;

        if (gstPercentage != null && gstPercentage.compareTo(BigDecimal.ZERO) > 0) {
            // If gstAmount is provided, use it
            if (gstAmount != null && gstAmount.compareTo(BigDecimal.ZERO) > 0) {
                calculatedGstAmount = gstAmount;
            } else {
                // Calculate GST on totalBeforeGst
                calculatedGstAmount = totalBeforeGstCalculated.multiply(gstPercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            // If totalBeforeGst is provided from frontend, use it
            if (totalBeforeGst != null && totalBeforeGst.compareTo(BigDecimal.ZERO) > 0) {
                totalBeforeGstCalculated = totalBeforeGst;
            }

            // Add GST to get final total
            calculatedTotal = calculatedTotal.add(calculatedGstAmount);
        }

        // Apply amount paid
        if (amountPaid != null) {
            calculatedTotal = calculatedTotal.subtract(amountPaid);
        }

    /* =====================================================
       STEP 6: SAVE NEW INVOICE WITH GST FIELDS
       ===================================================== */
        CustomerInvoiceRecord newInvoice = new CustomerInvoiceRecord();
        newInvoice.setInvoiceNumber(invoiceNumber);
        newInvoice.setCustomerName(customerName);
        newInvoice.setGrandTotal(calculatedTotal);
        newInvoice.setDiscount(invoiceDiscount != null ? invoiceDiscount : BigDecimal.ZERO);
        newInvoice.setRent(invoiceRent != null ? invoiceRent : BigDecimal.ZERO);
        newInvoice.setAmountPaid(amountPaid != null ? amountPaid : BigDecimal.ZERO);
        newInvoice.setTotalPurchaseCost(totalPurchaseCost); // ✅ NEW: Save total purchase cost
        newInvoice.setDescription(description);
        newInvoice.setBillingMonth(billingMonth);
        newInvoice.setIsActive(true);
        newInvoice.setStatus(status);

        // Set GST fields properly
        newInvoice.setGstPercentage(gstPercentage != null ? gstPercentage : BigDecimal.ZERO);
        newInvoice.setGstAmount(calculatedGstAmount);
        newInvoice.setTotalBeforeGst(totalBeforeGstCalculated);

        newInvoice.setBillingMonth(currentMonth);
        newInvoice.setSaleDate(LocalDateTime.now());
        newInvoice.setInvoiceDate(LocalDateTime.now());
        customerInvoiceRecordRepo.save(newInvoice);

    /* =====================================================
       STEP 8: SAVE PAYMENT ENTRY (OPTIONAL)
       ===================================================== */
        if (!"Return".equalsIgnoreCase(status)
                && amountPaid != null
                && amountPaid.compareTo(BigDecimal.ZERO) > 0) {

            Optional<CustomerPaymentTime> customerPaymentTime = customerPaymentTimeRepo.findByInvoiceNumberAndIsActive(invoiceNumber, true);
            if (customerPaymentTime.isPresent()) {
                CustomerPaymentTime customerPaymentTime1 = customerPaymentTime.get();
                customerPaymentTime1.setIsActive(false);
                customerPaymentTimeRepo.save(customerPaymentTime1);
            }

            CustomerPaymentTime payment = new CustomerPaymentTime();
            payment.setInvoiceNumber(invoiceNumber);
            payment.setCustomerName(customerName);
            payment.setAmountPaid(amountPaid);
            payment.setPaymentTime(LocalDateTime.now());
            payment.setBillingMonth(billingMonth);
            payment.setIsActive(true);

            customerPaymentTimeRepo.save(payment);
        }

        // Update customer bill with new total (including GST if applicable)
        updateCustomerBill(customerName, calculatedTotal, status, oldGrandTotal);

        return new Status(StatusMessage.SUCCESS,
                "Sale invoice updated successfully");
    }
    public Status deleteRecord(Integer invoiceNumber, String status) {
        List<SalesEntity> products = salesRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status,true);
        if (products == null || products.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Record is already deleted");
        }

        for (SalesEntity product : products) {
            Integer oldQuantity = product.getQuantity();
            Integer oldReturnedQuantity= product.getReturnedQuantity();

            InventoryEntity inventory = inventoryRepo.findFirstByCategoryAndProductNameOrderByAddedMonthDesc(product.getCategory(), product.getProductName());
            if (inventory != null) {
                if ("Sale".equals(status)){
                Integer newQuantity = inventory.getQuantity() + oldQuantity;
                inventory.setQuantity(newQuantity);
                inventory.setTotalPrice(calculateInventoryTotal(inventory));
                inventoryRepo.save(inventory);

                }else{
                    Integer newQuantity = inventory.getQuantity() - oldReturnedQuantity;
                    inventory.setQuantity(newQuantity);
                    inventory.setTotalPrice(calculateInventoryTotal(inventory));
                    inventoryRepo.save(inventory);
                }
            }

            product.setIsActive(false);
            product.setRecordDeletedTime(LocalDateTime.now());
            salesRepo.save(product);
        }
        Optional<CustomerInvoiceRecord> customerInvoiceAmountOpt = customerInvoiceRecordRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status,true);

        if (customerInvoiceAmountOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Record already deleted");
        }

        CustomerInvoiceRecord customerInvoiceRecord = customerInvoiceAmountOpt.get();
        customerInvoiceRecord.setIsActive(false);
        customerInvoiceRecordRepo.save(customerInvoiceRecord);

        CustomerBillAmountPaid customerBillAmountPaid = customerBillRepo.findByCustomerNameAndBillingMonth(customerInvoiceRecord.getCustomerName(),customerInvoiceRecord.getBillingMonth());
        if (customerBillAmountPaid != null) {
            if ("Sale".equals(status)){
                BigDecimal oldGrandTotal = customerInvoiceRecord.getGrandTotal();
                BigDecimal newBalance = customerBillAmountPaid.getBalance().subtract(oldGrandTotal);
                customerBillAmountPaid.setBalance(newBalance);
                customerBillRepo.save(customerBillAmountPaid);
            }else{
                BigDecimal oldGrandTotal = customerInvoiceRecord.getGrandTotal();
                BigDecimal newBalance = customerBillAmountPaid.getBalance().add(oldGrandTotal);
                customerBillAmountPaid.setBalance(newBalance);
                customerBillRepo.save(customerBillAmountPaid);
            }

        }

        Optional<CustomerPaymentTime> customerPaymentTime=customerPaymentTimeRepo.findByInvoiceNumberAndIsActive(invoiceNumber,true);

        if (customerPaymentTime.isPresent()){
            CustomerPaymentTime customerPaymentTime1= customerPaymentTime.get();
            customerPaymentTime1.setIsActive(false);
            customerPaymentTimeRepo.save(customerPaymentTime1);
        }


        return new Status(StatusMessage.SUCCESS, "Record is deleted");

    }



//    public Status getCustomerBalance(String customerName) {
//
//        // ================================
//        // 1. SINGLE CUSTOMER BALANCE CASE
//        // ================================
//        if (customerName != null && !customerName.trim().isEmpty()) {
//
//            CustomersBill latest = customerBillRepo.findTopByCustomerNameOrderByIdDesc(customerName);
//
//            if (latest == null) {
//                return new Status(StatusMessage.FAILURE, "No balance found for customer: " + customerName);
//            }
//
//            // Map entity → DTO
//            CustomerBalanceResponseDto dto = new CustomerBalanceResponseDto();
//            dto.setCustomerName(latest.getCustomerName());
//            dto.setCustomerBalance(latest.getBalance());
//
//            return new Status(StatusMessage.SUCCESS, dto);
//        }
//
//
//        // ==================================
//        // 2. ALL CUSTOMERS LATEST BALANCES
//        // ==================================
//        List<CustomersBill> balances = customerBillRepo.findLatestBalanceForAllCustomers();
//
//        if (balances == null || balances.isEmpty()) {
//            return new Status(StatusMessage.FAILURE, "No customer balances found");
//        }
//
//        // Convert list → List<CustomerBalanceResponseDto>
//        List<CustomerBalanceResponseDto> responseDtoList = balances.stream().map(c -> {
//
//            CustomerBalanceResponseDto dto = new CustomerBalanceResponseDto();
//            dto.setCustomerName(c.getCustomerName());
//            dto.setCustomerBalance(c.getBalance());
//
//            return dto;
//
//        }).collect(Collectors.toList());
//
//        return new Status(StatusMessage.SUCCESS, responseDtoList);
//    }

//    public Status getCustomerLedger(String customerName) {
//        if (customerName == null || customerName.trim().isEmpty()) {
//            return new Status(StatusMessage.FAILURE, "Customer name is required");
//        }
//
//        List<CustomersBill> ledger = customerBillRepo.findByCustomerName(customerName);
//
//        if (ledger.isEmpty()) {
//            return new Status(StatusMessage.FAILURE, "No records found for customer: " + customerName);
//        }
//
//        return new Status(StatusMessage.SUCCESS, ledger);
//    }

    public Status addVendor(VendorRequestDTO dto) {

        Vendor checkVendor = vendorRepository.findByVendorNameAndIsActiveTrue(dto.getVendorName());

        // Check if vendor exists
        if (checkVendor != null) {
            return new Status(StatusMessage.FAILURE,
                    "This Vendor is already present in record");
        }

        Vendor vendor = new Vendor();
        vendor.setVendorName(dto.getVendorName());
        vendor.setAddress(dto.getAddress());
        vendor.setPhoneNumber(dto.getPhoneNumber());
        vendor.setIsActive(true);

        vendorRepository.save(vendor);

        return new Status(StatusMessage.SUCCESS, "Vendor record saved");
    }

    public Status addCustomer(CustomerRequestDTO dto) {

        Customers checkCustomer = customerRepository.findByCustomerNameAndIsActiveTrue(dto.getCustomerName());

        // Check if vendor exists
        if (checkCustomer != null) {
            return new Status(StatusMessage.FAILURE,
                    "This Customer is already present in record");
        }

        Customers customers = new Customers();
        customers.setCustomerName(dto.getCustomerName());
        customers.setPhoneNumber(dto.getPhoneNumber());
        customers.setAddress(dto.getAddress());
        customers.setIsActive(true);

        customerRepository.save(customers);

        return new Status(StatusMessage.SUCCESS, "Customer record saved");
    }


    public Status getAllVendors() {
        List<Vendor> vendors = vendorRepository.findByIsActive(true);

        if (vendors.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No active vendors found");
        }

        return new Status(StatusMessage.SUCCESS, vendors);
    }

    public Status getAllCustomers() {
        List<Customers> customers = customerRepository.findByIsActive(true);

        if (customers.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No active customer found");
        }

        return new Status(StatusMessage.SUCCESS, customers);
    }

    public Status updateVendor(Long vendorId, VendorRequestDTO dto) {

        Optional<Vendor> optionalVendor = vendorRepository.findById(vendorId);

        if (!optionalVendor.isPresent()) {
            return new Status(StatusMessage.FAILURE, "Vendor not found");
        }

        Vendor vendor = optionalVendor.get();

        // Prevent duplicate vendor name
        Vendor existingVendor = vendorRepository.findByVendorNameAndIsActiveTrue(dto.getVendorName());
        if (existingVendor != null && !existingVendor.getId().equals(vendorId)) {
            return new Status(StatusMessage.FAILURE,
                    "Another vendor with this name already exists");
        }

        vendor.setVendorName(dto.getVendorName());
        vendor.setPhoneNumber(dto.getPhoneNumber());
        vendor.setAddress(dto.getAddress());
        vendor.setIsActive(true);

        vendorRepository.save(vendor);

        return new Status(StatusMessage.SUCCESS, "Vendor updated successfully");
    }

    public Status updateCustomer(Long customerId, CustomerRequestDTO dto) {

        Optional<Customers> optionalCustomers = customerRepository.findById(customerId);

        if (!optionalCustomers.isPresent()) {
            return new Status(StatusMessage.FAILURE, "Customer not found");
        }

        Customers customers = optionalCustomers.get();

        // Prevent duplicate vendor name
        Customers existingCustomers = customerRepository.findByCustomerNameAndIsActiveTrue(dto.getCustomerName());
        if (existingCustomers != null && !existingCustomers.getId().equals(customerId)) {
            return new Status(StatusMessage.FAILURE,
                    "Another customer with this name already exists");
        }

        customers.setCustomerName(dto.getCustomerName());
        customers.setPhoneNumber(dto.getPhoneNumber());
        customers.setAddress(dto.getAddress());
        customers.setIsActive(true);

        customerRepository.save(customers);

        return new Status(StatusMessage.SUCCESS, "Customer updated successfully");
    }

    public Status deleteVendor(Long vendorId) {

        Optional<Vendor> optionalVendor = vendorRepository.findById(vendorId);

        if (!optionalVendor.isPresent()) {
            return new Status(StatusMessage.FAILURE, "Vendor not found");
        }

        Vendor vendor = optionalVendor.get();

        if (!vendor.getIsActive()) {
            return new Status(StatusMessage.FAILURE, "Vendor already deleted");
        }

        vendor.setIsActive(false);
        vendorRepository.save(vendor);

        return new Status(StatusMessage.SUCCESS, "Vendor deleted successfully");
    }

    public Status deleteCustomer(Long customerId) {

        Optional<Customers> optionalCustomer = customerRepository.findById(customerId);

        if (!optionalCustomer.isPresent()) {
            return new Status(StatusMessage.FAILURE, "Customer not found");
        }

        Customers customers = optionalCustomer.get();

        if (!customers.getIsActive()) {
            return new Status(StatusMessage.FAILURE, "Customer already deleted");
        }

        customers.setIsActive(false);
        customerRepository.save(customers);

        return new Status(StatusMessage.SUCCESS, "Customer deleted successfully");
    }


}
