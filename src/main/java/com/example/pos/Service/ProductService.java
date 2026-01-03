package com.example.pos.Service;

import com.example.pos.DTO.*;
import com.example.pos.entity.pos.*;
import com.example.pos.repo.pos.*;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    @Autowired
    private CompanyStockReturnRepo companyStockReturnRepo;

    @Transactional
    public Status productAdded(List<ProductDto> productDtos, BigDecimal invoiceDiscount, BigDecimal invoiceRent, String description, String vendorName, BigDecimal payBill, String status) {

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

//            // product-level discount
//            if (productDto.getGeneralDiscount() != null) {
//                totalPrice = totalPrice.subtract(productDto.getGeneralDiscount());
//            }

            product.setTotalPrice(totalPrice);
            product.setStatus("Purchase");
            product.setIsActive(true);
            ProductEntity savedProduct = productRepo.save(product);
            savedProducts.add(savedProduct);

            // Update inventory
            InventoryEntity inventory = inventoryRepo.findByCategoryAndProductName(category.getCategoryName(), productDto.getProductName());
            if (inventory != null) {
                int newQty = inventory.getQuantity() + productDto.getQuantity();
                inventory.setQuantity(newQty);
                inventory.setPurchasePrice(productDto.getPrice());
                inventory.setTotalPrice(calculateInventoryTotal(inventory));

            } else {
                inventory = new InventoryEntity();
                inventory.setCategory(category.getCategoryName());
                inventory.setProductName(productDto.getProductName());
                inventory.setQuantity(productDto.getQuantity());
                inventory.setPurchasePrice(productDto.getPrice());
                inventory.setTotalPrice(totalPrice);
                inventory.setSize(productDto.getSize());
                inventory.setKtae(productDto.getKtae());
                inventory.setGram(productDto.getGram());
            }
            inventoryRepo.save(inventory);

            invoiceTotal = invoiceTotal.add(totalPrice); // add product total
        }

        // --- Apply invoice-level discount and rent ---
        if (invoiceDiscount != null) invoiceTotal = invoiceTotal.subtract(invoiceDiscount);
        if (invoiceRent != null) invoiceTotal = invoiceTotal.add(invoiceRent);
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
        companyInvoiceAmountRepo.save(invoiceAmount);

        // Update company/vendor bill
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


    private void handleCompanyPayment(String vendorName, BigDecimal payBill, int invoiceNumber) {

        YearMonth currentMonth = YearMonth.now();

        // --- Find current month bill ---
        CompanyBillAmountPaid bill =
                companyBillRepo.findByVendorNameAndBillingMonth(vendorName, currentMonth);

        if (bill == null) {
            // fallback: last month
            bill = companyBillRepo.findTopByVendorNameOrderByBillingMonthDesc(vendorName);

            if (bill == null) {
                throw new RuntimeException("No bill found for vendor: " + vendorName);
            }
        }

        // --- Minus payment from balance ---
        BigDecimal updatedBalance = bill.getBalance().subtract(payBill);

        bill.setBalance(payBill);
        companyBillRepo.save(bill);

        // --- Save payment history ---
        CompanyPaymentTime payment = new CompanyPaymentTime();
        payment.setInvoiceNumber(invoiceNumber);
        payment.setVendorName(vendorName);
        payment.setAmountPaid(payBill);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setBillingMonth(currentMonth);

        companyPaymentTimeRepo.save(payment);
    }


    @Transactional
    public Status updateStock(int invoiceNumber,
                              List<ProductDto> productDtos,
                              BigDecimal invoiceDiscount,
                              BigDecimal invoiceRent,
                              String description,
                              String vendorName,
                              BigDecimal payBill,
                              String status) {


        if (productDtos == null || productDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

        BigDecimal invoiceTotal = BigDecimal.ZERO;

        // 🔴 STEP 1: Remove existing products of this invoice (SOFT DELETE)
        List<ProductEntity> oldProducts =
                productRepo.findAllByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true);

        for (ProductEntity old : oldProducts) {


            // 🔴 inventory se purani quantity minus (LOGIC SAME – sirf add)
            InventoryEntity oldInventory =
                    inventoryRepo.findByCategoryAndProductName(
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
            }else{
            product.setReturnTime(null);}
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
                    inventoryRepo.findByCategoryAndProductName(
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
        companyInvoiceAmountRepo.save(invoiceAmount);

        updateCompanyBill(vendorName, invoiceTotal, status, olTotal);


        if (payBill != null && payBill.compareTo(BigDecimal.ZERO) > 0) {

            YearMonth currentMonth = YearMonth.now();

            Optional<CompanyPaymentTime> companyPaymentTime=companyPaymentTimeRepo.findByInvoiceNumberAndIsActive(invoiceNumber,true);
            if (companyPaymentTime.isPresent()){
                CompanyPaymentTime companyPaymentTime1=companyPaymentTime.get();
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

    public Status searchProducts(String status, String category, String productName) {

        boolean categoryEmpty = (category == null || category.isBlank());
        boolean productEmpty = (productName == null || productName.isBlank());

        List<ProductEntity> products = new ArrayList<>();

        // --- Fetch products using your existing logic ---
        if (categoryEmpty && productEmpty) {
            products = productRepo.findByIsActiveTrueAndStatus(status);
            if (products.isEmpty()) return new Status(StatusMessage.FAILURE, "No products found");
        } else if (!categoryEmpty && !productEmpty) {
            Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName);
            if (pnOpt.isEmpty()) return new Status(StatusMessage.FAILURE, "Product name not found");
            String pnIdStr = String.valueOf(pnOpt.get().getId());
            products = productRepo.findByCategoryAndProductName(category, pnIdStr);
        } else if (!productEmpty) {
            Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName);
            if (pnOpt.isEmpty()) return new Status(StatusMessage.FAILURE, "Product name not found");
            String pnIdStr = String.valueOf(pnOpt.get().getId());
            products = productRepo.findByProductName(pnIdStr);
        } else if (!categoryEmpty) {
            products = productRepo.findByCategory(category);
        }

        if (products == null || products.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No matching products found");
        }

        // --- Group products by invoiceNumber ---
        Map<Integer, List<ProductDto>> invoiceMap = new HashMap<>();

        for (ProductEntity p : products) {

            ProductDto dto = new ProductDto();
            String actualProductName = p.getProductName();

            try {
                Long pid = Long.valueOf(p.getProductName());
                actualProductName = productNameRepository.findById(pid)
                        .map(ProductName::getProductName)
                        .orElse(p.getProductName());
            } catch (NumberFormatException ignored) {
            }

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
                    Optional<CompanyInvoiceAmount> ciaOpt = companyInvoiceAmountRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNo, status, true);

                    if (ciaOpt.isPresent()) {
                        CompanyInvoiceAmount cia = ciaOpt.get();
                        invoiceDto.setVendorName(cia.getVendorName());
                        invoiceDto.setInvoiceDiscount(cia.getDiscount());
                        invoiceDto.setInvoiceRent(cia.getRent());
                        invoiceDto.setAmountPaid(cia.getAmountPaid());
                        invoiceDto.setDescription(cia.getDescription());
                        invoiceDto.setGrandTotal(cia.getGrandTotal());
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

                InventoryEntity inventory = inventoryRepo.findByCategoryAndProductName(product.getCategory(), product.getProductName());

                if (inventory != null) {
                    Integer newQuantity = inventory.getQuantity() - oldQuantity;
                    inventory.setQuantity(newQuantity);
                    inventory.setTotalPrice(calculateInventoryTotal(inventory));
                    inventoryRepo.save(inventory);
                }
            } else {
                InventoryEntity inventory = inventoryRepo.findByCategoryAndProductName(product.getCategory(), product.getProductName());
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
                    dto.setProductPrice(product.getProductPrice());

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
            List<RevertDto> revertDtos) {

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
                    inventoryRepo.findByCategoryAndProductName(
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

        if (invoiceDiscount != null) totalRevertAmount = totalRevertAmount.subtract(invoiceDiscount);
        if (invoiceRent != null) totalRevertAmount = totalRevertAmount.add(invoiceRent);

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

        bill.setBalance(
                bill.getBalance().subtract(totalRevertAmount)
        );

        companyBillRepo.save(bill);

        return new Status(
                StatusMessage.SUCCESS,
                "Stock reverted successfully"
        );
    }


}

