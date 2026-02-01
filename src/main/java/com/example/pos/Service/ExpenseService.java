package com.example.pos.Service;

import com.example.pos.DTO.ExpenseCategoryDto;
import com.example.pos.DTO.ExpenseDto;

import com.example.pos.entity.pos.ExpenseCategory;
import com.example.pos.entity.pos.Expenses;

import com.example.pos.repo.pos.ExpenseCategoryRepository;
import com.example.pos.repo.pos.ExpenseRepository;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseCategoryRepository repository;

    public Status addExpense(List<ExpenseDto> dtos) {

        if (dtos == null || dtos.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No expense data provided");
        }

        List<Expenses> expenses = dtos.stream().map(dto -> {
            Expenses expense = new Expenses();
            expense.setCategory(dto.getCategory());
            expense.setAmount(dto.getAmount());

            // Determine expense type (default to DAILY)
            String type = dto.getExpenseType() != null ? dto.getExpenseType().toUpperCase() : "DAILY";
            expense.setExpenseType(type);
            expense.setExpenseDate(dto.getExpenseDate());

            expense.setExpenseTime(LocalTime.now());
            expense.setDescription(dto.getDescription());
            expense.setIsActive(true);

            return expense;
        }).toList();

        expenseRepository.saveAll(expenses);

        return new Status(StatusMessage.SUCCESS, "Expenses saved successfully");
    }


    public Object getExpenses(Optional<LocalDate> startDate,
                              Optional<LocalDate> endDate) {

        // 🔹 CASE 1: Start + End date provided
        if (startDate.isPresent() && endDate.isPresent()) {

            List<Expenses> expenses =
                    expenseRepository.findByExpenseDateBetweenAndIsActiveOrderByExpenseDateAsc(
                            startDate.get(), endDate.get(), true);

            // Group by date
            return expenses.stream()
                    .collect(Collectors.groupingBy(
                            Expenses::getExpenseDate,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
        }

        // 🔹 CASE 2: No dates → latest date expenses only
        LocalDate latestDate = expenseRepository.findLatestExpenseDateAndIsActive(true);

        if (latestDate == null) {
            return Collections.emptyList();
        }

        return Map.of(
                latestDate,
                expenseRepository.findByExpenseDateAndIsActive(latestDate,true)
        );
    }


    // Add category
    public Status addCategory(ExpenseCategoryDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty() || dto.getType() == null || dto.getType().trim().isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Category name and type are required");
        }

        if (repository.findByNameIgnoreCaseAndIsActiveTrue(dto.getName().trim()).isPresent()) {
            return new Status(StatusMessage.FAILURE, "Category already exists");
        }

        ExpenseCategory category = new ExpenseCategory();
        category.setName(dto.getName().trim());
        category.setType(dto.getType().trim().toUpperCase()); // Normalize type
        category.setIsActive(true);

        repository.save(category);
        return new Status(StatusMessage.SUCCESS, "Category added successfully");
    }

    // Get categories by type
    public List<ExpenseCategoryDto> getCategoriesByType(String type) {
        return repository.findByTypeAndIsActiveTrue(type.toUpperCase())
                .stream()
                .map(c -> {
                    ExpenseCategoryDto dto = new ExpenseCategoryDto();
                    dto.setId(c.getId());
                    dto.setName(c.getName());
                    dto.setType(c.getType());
                    return dto;
                }).collect(Collectors.toList());
    }

    public Status updateCategory(Long id, ExpenseCategoryDto dto) {

        ExpenseCategory category = repository.findByIdAndIsActive(id,true)
                .orElseThrow(() -> new RuntimeException("Category not found"));


        category.setName(dto.getName());
        category.setType(dto.getType());

        repository.save(category);

        return new Status(StatusMessage.SUCCESS, "Category updated successfully");
    }

    public Status deleteCategory(Long id){
        ExpenseCategory category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category already deleted"));

        category.setIsActive(false);

        repository.save(category);
        return new Status(StatusMessage.SUCCESS, "Category deleted");
    }

    public Status updateExpense(Long id, ExpenseDto dto) {
        Optional<Expenses> opt = expenseRepository.findById(id);
        if (opt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Expense not found");
        }

        Expenses expense = opt.get();
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setExpenseType(dto.getExpenseType() != null ? dto.getExpenseType().toUpperCase() : "DAILY");
        if (dto.getExpenseDate() != null) {
            expense.setExpenseDate(dto.getExpenseDate());
        }

        expenseRepository.save(expense);
        return new Status(StatusMessage.SUCCESS, "Expense updated successfully");
    }

    // Delete expense
    public Status deleteExpense(Long id) {

        Optional<Expenses> expenses= expenseRepository.findById(id);
        if (expenses.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Expense not found");
        }
        Expenses expenses1= expenses.get();
        expenses1.setIsActive(false);
        expenseRepository.save(expenses1);
        return new Status(StatusMessage.SUCCESS, "Expense deleted successfully");
    }

}
