package com.example.pos.Controller;

import com.example.pos.DTO.ExpenseCategoryDto;
import com.example.pos.DTO.ExpenseDto;
import com.example.pos.Service.ExpenseService;
import com.example.pos.entity.pos.Expenses;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/Expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping("/add")
    public Status addExpenses(@RequestBody List<ExpenseDto> dtos) {
        return expenseService.addExpense(dtos);
    }

    @GetMapping("/get/expenses")
    public ResponseEntity<?> getExpenses(
            @RequestParam Optional<LocalDate> startDate,
            @RequestParam Optional<LocalDate> endDate) {

        return ResponseEntity.ok(expenseService.getExpenses(startDate, endDate));
    }


    @PostMapping("/addCategory")
    public Status addCategory(@RequestBody ExpenseCategoryDto categoryDto){
        return expenseService.addCategory(categoryDto);
    }

    // Get categories by type
    @GetMapping("/get/category")
    public List<ExpenseCategoryDto> getCategories(@RequestParam(value = "type", required = false) String type) {
        if (type != null) {
            return expenseService.getCategoriesByType(type);
        } else {
            return expenseService.getCategoriesByType("DAILY"); // default to DAILY if type not specified
        }
    }

    @PutMapping("/updateCategory/{id}")
    public Status updateCategory(
            @PathVariable Long id,
            @RequestBody ExpenseCategoryDto dto
    ) {
        return expenseService.updateCategory(id, dto);
    }

    @DeleteMapping("/category/delete/{id}")
    public Status deleteCategory(@PathVariable Long id){
        return expenseService.deleteCategory(id);
    }

    @PutMapping("/updateExpenses/{id}")
    public Status updateExpense(@PathVariable Long id, @RequestBody ExpenseDto dto) {
        return expenseService.updateExpense(id, dto);
    }

    // ---------------- Delete Expense ----------------
    @DeleteMapping("/delete/{id}")
    public Status deleteExpense(@PathVariable Long id) {
        return expenseService.deleteExpense(id);
    }

}
