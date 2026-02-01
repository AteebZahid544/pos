package com.example.pos.repo.pos;

import com.example.pos.entity.pos.EmployeeSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSalaryRepository
        extends JpaRepository<EmployeeSalary, Long> {

    Optional<EmployeeSalary> findByEmployeeIdAndSalaryMonthAndPaymentTypeAndIsActive(
            Long employeeId,
            LocalDate salaryMonth,
            String paymentType,
            boolean isActive
    );

    Optional<EmployeeSalary> findTopByEmployeeIdAndIsActiveOrderByIdDesc(Long employId, boolean isActive);


    List<EmployeeSalary>findByEmployeeIdAndIsActiveTrue(Long employeeId);

    List<EmployeeSalary> findByEmployeeIdAndPaymentTypeAndStatusAndIsActiveTrue(
            Long employeeId,
            String paymentType,  // "SALARY"
            String status        // "PAID"
    );

    // ✅ Active paid advances ordered by paidOn
    List<EmployeeSalary> findByEmployeeIdAndPaymentTypeAndIsActiveTrueOrderByPaidOnAsc(
            Long employeeId,
            String paymentType
    );

    @Query("SELECT COALESCE(SUM(" +
            "COALESCE(s.totalPaid, 0) + " +
            "COALESCE(s.advanceGiven, 0) + " +
            "COALESCE(s.overtime, 0) + " +
            "COALESCE(s.bonus, 0) - " +
            "COALESCE(s.deduction, 0)" +
            "), 0) " +
            "FROM EmployeeSalary s " +
            "WHERE ((FUNCTION('YEAR', COALESCE(s.salaryDate, s.paidOn)) = :year " +
            "AND FUNCTION('MONTH', COALESCE(s.salaryDate, s.paidOn)) = :month) " +
            "OR (s.salaryDate IS NULL AND FUNCTION('YEAR', s.paidOn) = :year " +
            "AND FUNCTION('MONTH', s.paidOn) = :month)) " +
            "AND s.isActive = true")
    Double getActualSalaryCostByMonth(
            @Param("year") int year,
            @Param("month") int month
    );

}
