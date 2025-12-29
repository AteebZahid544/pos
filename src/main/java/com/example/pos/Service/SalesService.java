package com.example.pos.Service;

import com.example.pos.DTO.*;
import com.example.pos.entity.pos.*;
import com.example.pos.repo.pos.*;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    public Status productSold(List<SalesDto> productDtos,
                              BigDecimal invoiceDiscount,
                              BigDecimal invoiceRent,
                              String description,
                              String customerName,
                              BigDecimal payBill,
                              String status) {

        if (productDtos == null || productDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

        BigDecimal invoiceTotal = BigDecimal.ZERO;
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

            // 🔴 INVENTORY CHECK
            InventoryEntity inventory =
                    inventoryRepo.findByCategoryAndProductName(
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
            product.setInvoiceNumber(invoiceNumber);
            if ("Sale".equals(status)) {
                product.setQuantity(productDto.getQuantity());
                product.setSaleEntryTime(LocalDateTime.now());
            } else {
                product.setReturnedQuantity(productDto.getReturnedQuantity());
                product.setReturnTime(LocalDateTime.now());
            }
            product.setIsActive(true);

            BigDecimal totalPrice;

            if("Sale".equals(status)){
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

            } else if ("roll".equalsIgnoreCase(category.getCategoryName())
                    || "reel".equalsIgnoreCase(category.getCategoryName())) {

                BigDecimal size = productDto.getSize() != null ? productDto.getSize() : BigDecimal.ONE;
                totalPrice = size
                        .multiply(BigDecimal.valueOf(productDto.getQuantity()))
                        .multiply(productDto.getPrice());

                product.setSize(size);

            } else {
                totalPrice = productDto.getPrice()
                        .multiply(BigDecimal.valueOf(productDto.getQuantity()));
            }
                product.setTotalPrice(totalPrice);}
            else {
                if ("packet".equalsIgnoreCase(category.getCategoryName())) {

                    Integer quantity = productDto.getReturnedQuantity();
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

                } else if ("roll".equalsIgnoreCase(category.getCategoryName())
                        || "reel".equalsIgnoreCase(category.getCategoryName())) {

                    BigDecimal size = productDto.getSize() != null ? productDto.getSize() : BigDecimal.ONE;
                    totalPrice = size
                            .multiply(BigDecimal.valueOf(productDto.getReturnedQuantity()))
                            .multiply(productDto.getPrice());

                    product.setSize(size);

                } else {
                    totalPrice = productDto.getPrice()
                            .multiply(BigDecimal.valueOf(productDto.getReturnedQuantity()));
                }
                product.setTotalPrice(totalPrice);
            }

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
        }

        // --- Invoice discount & rent ---
        if (invoiceDiscount != null) invoiceTotal = invoiceTotal.subtract(invoiceDiscount);
        if (invoiceRent != null) invoiceTotal = invoiceTotal.add(invoiceRent);
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

        if (payBill != null) {
            invoiceAmount.setAmountPaid(payBill);
        }

        customerInvoiceRecordRepo.save(invoiceAmount);

        updateCustomerBill(customerName, invoiceTotal, status, null);

        // --- Handle payment ---
        if (payBill != null && payBill.compareTo(BigDecimal.ZERO) > 0) {
            handleCustomerPayment(customerName, payBill, invoiceNumber);
        }

        return new Status(StatusMessage.SUCCESS, "Products sold successfully");
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


    private void updateCustomerBill(String customer, BigDecimal totalPrice, String status, BigDecimal oldTotal) {

        YearMonth currentMonth = YearMonth.now();

        // Check if current month bill exists
        CustomerBillAmountPaid currentMonthRecord =
                customerBillRepo.findByCustomerNameAndBillingMonth(customer, currentMonth);

        if (currentMonthRecord != null) {
            if("Sale".equals(status)){
                if (oldTotal!=null){

                    BigDecimal updatedBalance = currentMonthRecord.getBalance().subtract(oldTotal);
                    currentMonthRecord.setBalance(updatedBalance);
                    customerBillRepo.save(currentMonthRecord);
                }

            BigDecimal updatedBalance = currentMonthRecord.getBalance().add(totalPrice);
            currentMonthRecord.setBalance(updatedBalance);
            customerBillRepo.save(currentMonthRecord);
            return;
        }else{
                if (oldTotal!=null){

                        BigDecimal updatedBalance = currentMonthRecord.getBalance().add(oldTotal);
                        currentMonthRecord.setBalance(updatedBalance);
                        customerBillRepo.save(currentMonthRecord);
                }
                BigDecimal updatedBalance = currentMonthRecord.getBalance().subtract(totalPrice);
                currentMonthRecord.setBalance(updatedBalance);
                customerBillRepo.save(currentMonthRecord);
                return;
            }
        }

        // No current month record → fetch last month’s balance
        CustomerBillAmountPaid lastMonthRecord =
                customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(customer);

        BigDecimal carryForwardBalance = (lastMonthRecord != null)
                ? lastMonthRecord.getBalance()
                : BigDecimal.ZERO;

        // Create new monthly bill record
        CustomerBillAmountPaid newBill = new CustomerBillAmountPaid();
        newBill.setCustomerName(customer);
        newBill.setBillingMonth(currentMonth);
        newBill.setBalance(carryForwardBalance.add(totalPrice));

        customerBillRepo.save(newBill);
    }

    private void handleCustomerPayment(String customerName,
                                       BigDecimal payBill,
                                       int invoiceNumber) {
        YearMonth currentMonth = YearMonth.now();

        // --- Find current month bill ---
        CustomerBillAmountPaid bill =
                customerBillRepo.findByCustomerNameAndBillingMonth(customerName, currentMonth);

        if (bill == null) {
            // fallback: last month
            bill = customerBillRepo.findTopByCustomerNameOrderByBillingMonthDesc(customerName);

            if (bill == null) {
                throw new RuntimeException("No bill found for customer: " + customerName);
            }
        }

        // --- Minus payment from balance ---
        BigDecimal updatedBalance = bill.getBalance().subtract(payBill);

        bill.setBalance(updatedBalance);
        customerBillRepo.save(bill);

        // --- Save payment history ---
        CustomerPaymentTime payment = new CustomerPaymentTime();
        payment.setInvoiceNumber(invoiceNumber);
        payment.setCustomerName(customerName);
        payment.setAmountPaid(payBill);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setBillingMonth(currentMonth);

        customerPaymentTimeRepo.save(payment);
    }

    public Status searchProducts(String category, String productName, String status) {

        boolean categoryEmpty = (category == null || category.isBlank());
        boolean productEmpty = (productName == null || productName.isBlank());

        List<SalesEntity> products = new ArrayList<>();

        // --- Fetch products using your existing logic ---
        if (categoryEmpty && productEmpty) {
            products = salesRepo.findByIsActiveTrueAndStatus(status);
            if (products.isEmpty()) return new Status(StatusMessage.FAILURE, "No products found");
        } else if (!categoryEmpty && !productEmpty) {
            Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName);
            if (pnOpt.isEmpty()) return new Status(StatusMessage.FAILURE, "Product name not found");
            String pnIdStr = String.valueOf(pnOpt.get().getId());
            products = salesRepo.findByCategoryAndProductName(category, pnIdStr);
        } else if (!productEmpty) {
            Optional<ProductName> pnOpt = productNameRepository.findByProductNameIgnoreCase(productName);
            if (pnOpt.isEmpty()) return new Status(StatusMessage.FAILURE, "Product name not found");
            String pnIdStr = String.valueOf(pnOpt.get().getId());
            products = salesRepo.findByProductName(pnIdStr);
        } else if (!categoryEmpty) {
            products = salesRepo.findByCategory(category);
        }

        if (products == null || products.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No matching products found");
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
            dto.setReturnTime(String.valueOf(s.getReturnTime()));

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

                    Optional<CustomerInvoiceRecord> ciaOpt = customerInvoiceRecordRepo.findByInvoiceNumberAndStatusAndIsActive(invoiceNo, status,true);

                    if (ciaOpt.isPresent()) {
                        CustomerInvoiceRecord cia = ciaOpt.get();
                        invoiceDto.setCustomerName(cia.getCustomerName());
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


    @Transactional
    public Status cancelProductSale(int id) {
        Optional<SalesEntity> optionalSale = salesRepo.findById(id);
        if (optionalSale.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Sale record not found");
        }

        SalesEntity sale = optionalSale.get();
        InventoryEntity products = inventoryRepo.findByCategoryAndProductName(sale.getCategory(), sale.getProductName());

        if (products == null) {
            return new Status(StatusMessage.FAILURE, "Product not found");
        }

        Integer restoredQuantity = products.getQuantity() + sale.getQuantity();
        products.setQuantity(restoredQuantity);
        inventoryRepo.save(products);

        salesRepo.deleteById(id);

        return new Status(StatusMessage.SUCCESS, "Sale cancelled and stock restored successfully");
    }

    @Transactional
    public Status updateSaleEntry(int invoiceNumber,
                                  List<SalesDto> productDtos,
                                  BigDecimal invoiceDiscount,
                                  BigDecimal invoiceRent,
                                  String description,
                                  String customerName,
                                  BigDecimal amountPaid,
                                  String status) {

        if (productDtos == null || productDtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product details are missing");
        }

    /* =====================================================
       STEP 1: FETCH OLD INVOICE + OLD SALES
       ===================================================== */
        CustomerInvoiceRecord oldInvoice =
                customerInvoiceRecordRepo
                        .findByInvoiceNumberAndStatusAndIsActive(invoiceNumber, status, true)
                        .orElseThrow(() ->
                                new RuntimeException("Invoice not found"));
        BigDecimal oldGrandTotal= oldInvoice.getGrandTotal();

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
                    inventoryRepo.findByCategoryAndProductName(
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

    /* =====================================================
       STEP 3: REVERSE OLD INVOICE EFFECT FROM CUSTOMER BILL
       ===================================================== */
        if (!"Return".equalsIgnoreCase(status)) {

            BigDecimal oldPaid =
                    oldInvoice.getAmountPaid() != null
                            ? oldInvoice.getAmountPaid()
                            : BigDecimal.ZERO;

            BigDecimal oldActualAmount =
                    oldInvoice.getGrandTotal().subtract(oldPaid);

            CustomerBillAmountPaid oldBill =
                    customerBillRepo.findByCustomerNameAndBillingMonth(
                            oldInvoice.getCustomerName(),
                            billingMonth
                    );

            if (oldBill != null) {
                oldBill.setBalance(
                        oldBill.getBalance().subtract(oldActualAmount)
                );
                customerBillRepo.save(oldBill);
            }
        }

        oldInvoice.setIsActive(false);
        customerInvoiceRecordRepo.save(oldInvoice);

    /* =====================================================
       STEP 4: APPLY NEW SALES (INVENTORY MINUS)
       ===================================================== */
        BigDecimal newInvoiceTotal = BigDecimal.ZERO;

        for (SalesDto dto : productDtos) {

            SalesEntity sale = new SalesEntity();
            sale.setInvoiceNumber(invoiceNumber);
            sale.setCategory(dto.getCategory());
            sale.setProductName(dto.getProductName());
            sale.setPrice(dto.getPrice());
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
            String category = dto.getCategory().toLowerCase();

            int qty =
                    "Return".equalsIgnoreCase(status)
                            ? (dto.getReturnedQuantity() != null ? dto.getReturnedQuantity() : 0)
                            : (dto.getQuantity() != null ? dto.getQuantity() : 0);

            if ("packet".equals(category)) {
                totalPrice = BigDecimal.valueOf(qty)
                        .multiply(dto.getSize())
                        .multiply(dto.getKtae())
                        .multiply(dto.getGram())
                        .multiply(dto.getPrice())
                        .divide(BigDecimal.valueOf(15500), 2, RoundingMode.HALF_UP);
            } else if ("roll".equals(category) || "reel".equals(category)) {
                totalPrice = dto.getSize()
                        .multiply(BigDecimal.valueOf(qty))
                        .multiply(dto.getPrice());
            } else {
                totalPrice = dto.getPrice()
                        .multiply(BigDecimal.valueOf(qty));
            }

            if (dto.getGeneralDiscount() != null) {
                totalPrice = totalPrice.subtract(dto.getGeneralDiscount());
            }

            sale.setTotalPrice(totalPrice);
            salesRepo.save(sale);

            InventoryEntity inventory =
                    inventoryRepo.findByCategoryAndProductName(
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

            newInvoiceTotal = newInvoiceTotal.add(totalPrice);
        }

    /* =====================================================
       STEP 5: APPLY DISCOUNT & RENT
       ===================================================== */
        if (invoiceDiscount != null)
            newInvoiceTotal = newInvoiceTotal.subtract(invoiceDiscount);

        if (invoiceRent != null)
            newInvoiceTotal = newInvoiceTotal.add(invoiceRent);

        if (amountPaid != null) newInvoiceTotal = newInvoiceTotal.subtract(amountPaid);


    /* =====================================================
       STEP 6: SAVE NEW INVOICE
       ===================================================== */
        CustomerInvoiceRecord newInvoice = new CustomerInvoiceRecord();
        newInvoice.setInvoiceNumber(invoiceNumber);
        newInvoice.setCustomerName(customerName);
        newInvoice.setGrandTotal(newInvoiceTotal);
        newInvoice.setDiscount(invoiceDiscount != null ? invoiceDiscount : BigDecimal.ZERO);
        newInvoice.setRent(invoiceRent != null ? invoiceRent : BigDecimal.ZERO);
        newInvoice.setAmountPaid(amountPaid != null ? amountPaid : BigDecimal.ZERO);
        newInvoice.setDescription(description);
        newInvoice.setBillingMonth(billingMonth);
        newInvoice.setIsActive(true);
        newInvoice.setStatus(status);
        customerInvoiceRecordRepo.save(newInvoice);

    /* =====================================================
       STEP 7: APPLY NEW BILL BALANCE
       ===================================================== */
        if (!"Return".equalsIgnoreCase(status)) {

            BigDecimal paid =
                    amountPaid != null ? amountPaid : BigDecimal.ZERO;

            BigDecimal newActualAmount =
                    newInvoiceTotal.subtract(paid);

            CustomerBillAmountPaid newBill =
                    customerBillRepo.findByCustomerNameAndBillingMonth(
                            customerName, billingMonth);

            if (newBill == null) {
                newBill = new CustomerBillAmountPaid();
                newBill.setCustomerName(customerName);
                newBill.setBillingMonth(billingMonth);
                newBill.setBalance(newActualAmount);
            } else {
                newBill.setBalance(
                        newBill.getBalance().add(newActualAmount)
                );
            }

            customerBillRepo.save(newBill);
        }

    /* =====================================================
       STEP 8: SAVE PAYMENT ENTRY (OPTIONAL)
       ===================================================== */
        if (!"Return".equalsIgnoreCase(status)
                && amountPaid != null
                && amountPaid.compareTo(BigDecimal.ZERO) > 0) {

            CustomerPaymentTime payment = new CustomerPaymentTime();
            payment.setInvoiceNumber(invoiceNumber);
            payment.setCustomerName(customerName);
            payment.setAmountPaid(amountPaid);
            payment.setPaymentTime(LocalDateTime.now());
            payment.setBillingMonth(billingMonth);

            customerPaymentTimeRepo.save(payment);
        }
        updateCustomerBill(customerName,newInvoiceTotal,status,oldGrandTotal);

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

            InventoryEntity inventory = inventoryRepo.findByCategoryAndProductName(product.getCategory(), product.getProductName());
            if (inventory != null) {
                Integer newQuantity = inventory.getQuantity() + oldQuantity;
                inventory.setQuantity(newQuantity);
                inventoryRepo.save(inventory);
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

        CustomerBillAmountPaid customerBillAmountPaid = customerBillRepo.findByCustomerName(customerInvoiceRecord.getCustomerName());
        if (customerBillAmountPaid != null) {
            BigDecimal oldGrandTotal = customerInvoiceRecord.getGrandTotal().subtract(customerInvoiceRecord.getAmountPaid());
            BigDecimal newBalance = customerBillAmountPaid.getBalance().subtract(oldGrandTotal);
            customerBillAmountPaid.setBalance(newBalance);
            customerBillRepo.save(customerBillAmountPaid);
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

        Vendor checkVendor = vendorRepository.findByVendorName(dto.getVendorName());

        // Check if vendor exists
        if (checkVendor != null) {
            return new Status(StatusMessage.FAILURE,
                    "This Vendor is already present in record");
        }

        Vendor vendor = new Vendor();
        vendor.setVendorName(dto.getVendorName());
        vendor.setAddress(dto.getAddress());
        vendor.setIsActive(true);

        vendorRepository.save(vendor);

        return new Status(StatusMessage.SUCCESS, "Vendor record saved");
    }

    public Status addCustomer(CustomerRequestDTO dto) {

        Customers checkCustomer = customerRepository.findByCustomerName(dto.getCustomerName());

        // Check if vendor exists
        if (checkCustomer != null) {
            return new Status(StatusMessage.FAILURE,
                    "This Customer is already present in record");
        }

        Customers customers = new Customers();
        customers.setCustomerName(dto.getCustomerName());
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
        Vendor existingVendor = vendorRepository.findByVendorName(dto.getVendorName());
        if (existingVendor != null && !existingVendor.getId().equals(vendorId)) {
            return new Status(StatusMessage.FAILURE,
                    "Another vendor with this name already exists");
        }

        vendor.setVendorName(dto.getVendorName());
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
        Customers existingCustomers = customerRepository.findByCustomerName(dto.getCustomerName());
        if (existingCustomers != null && !existingCustomers.getId().equals(customerId)) {
            return new Status(StatusMessage.FAILURE,
                    "Another customer with this name already exists");
        }

        customers.setCustomerName(dto.getCustomerName());
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
