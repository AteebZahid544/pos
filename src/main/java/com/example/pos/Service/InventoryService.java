package com.example.pos.Service;

import com.example.pos.entity.pos.CompanyPaymentTime;
import com.example.pos.entity.pos.CustomerPaymentTime;
import com.example.pos.entity.pos.InventoryEntity;

import com.example.pos.repo.pos.CompanyPaymentTimeRepo;
import com.example.pos.repo.pos.CustomerPaymentTimeRepo;
import com.example.pos.repo.pos.InventoryRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepo inventoryRepo;
    private final CustomerPaymentTimeRepo customerPaymentTimeRepo;
    private final CompanyPaymentTimeRepo companyPaymentTime;

    public Status getInventoryGroupedByCategory(String category) {

        List<InventoryEntity> inventoryList =
                inventoryRepo.findAllByOrderByCategoryAsc();

        if (inventoryList == null || inventoryList.isEmpty()) {
            return new Status(
                    StatusMessage.FAILURE,
                    "No inventory found"
            );
        }

        // If category is provided → filter first
        if (category != null && !category.trim().isEmpty()) {

            String requestedCategory = category.trim().toLowerCase();

            List<InventoryEntity> filteredList = inventoryList.stream()
                    .filter(inv ->
                            inv.getCategory() != null &&
                                    inv.getCategory().equalsIgnoreCase(requestedCategory)
                    )
                    .collect(Collectors.toList());

            if (filteredList.isEmpty()) {
                return new Status(
                        StatusMessage.FAILURE,
                        "No inventory found for category: " + category
                );
            }

            // Return only requested category
            Map<String, List<InventoryEntity>> singleCategoryMap = new HashMap<>();
            singleCategoryMap.put(requestedCategory, filteredList);

            return new Status(
                    StatusMessage.SUCCESS,
                    singleCategoryMap
            );
        }

        // No category → return all grouped data
        Map<String, List<InventoryEntity>> groupedInventory =
                inventoryList.stream()
                        .collect(Collectors.groupingBy(
                                inv -> inv.getCategory().toLowerCase()
                        ));

        return new Status(
                StatusMessage.SUCCESS,
                groupedInventory
        );
    }


    public Status getAllPaymentsGroupedByCustomer() {

        List<CustomerPaymentTime> payments =
                customerPaymentTimeRepo.findAll();

        if (payments == null || payments.isEmpty()) {
            return new Status(
                    StatusMessage.FAILURE,
                    "No payment records found"
            );
        }

        Map<String, List<CustomerPaymentTime>> groupedByCustomer =
                payments.stream()
                        .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                        .collect(Collectors.groupingBy(
                                CustomerPaymentTime::getCustomerName
                        ));

        return new Status(
                StatusMessage.SUCCESS,
                groupedByCustomer
        );
    }

    public Status getAllPaymentsGroupedByVendor() {

        List<CompanyPaymentTime> payments =
                companyPaymentTime.findAll();

        if (payments == null || payments.isEmpty()) {
            return new Status(
                    StatusMessage.FAILURE,
                    "No payment records found"
            );
        }

        Map<String, List<CompanyPaymentTime>> groupedByVendors =
                payments.stream()
                        .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                        .collect(Collectors.groupingBy(
                                CompanyPaymentTime::getVendorName
                        ));

        return new Status(
                StatusMessage.SUCCESS,
                groupedByVendors
        );
    }

}
