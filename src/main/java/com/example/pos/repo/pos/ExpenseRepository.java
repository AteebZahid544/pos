package com.example.pos.repo.pos;



import com.example.pos.entity.pos.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expenses, Long> {
    // Latest expense date
    @Query("SELECT MAX(e.expenseDate) FROM Expenses e")
    LocalDate findLatestExpenseDateAndIsActive(Boolean isActive);

    // Expenses by exact date
    List<Expenses> findByExpenseDateAndIsActive(LocalDate expenseDate, boolean isActive);

    // Expenses between dates (sorted)
    List<Expenses> findByExpenseDateBetweenAndIsActiveOrderByExpenseDateAsc(
            LocalDate start, LocalDate end, Boolean isActive);

    @Query("""
    SELECT COALESCE(SUM(e.amount), 0)
    FROM Expenses e
    WHERE e.isActive = true
      AND e.expenseDate BETWEEN :startDate AND :endDate
""")
    Double getTotalExpensesBetweenDates(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Query("SELECT e FROM Expenses e " +
            "WHERE e.isActive = true " +
            "AND e.expenseDate = :date " +
            "AND e.expenseType = 'DAILY' " + // Only get daily expenses
            "ORDER BY e.expenseTime DESC")
    List<Expenses> findDailyExpensesByDate(@Param("date") LocalDate date);

}
