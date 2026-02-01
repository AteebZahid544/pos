package com.example.pos.Service;

import com.example.pos.DTO.*;

import com.example.pos.entity.pos.Employee;
import com.example.pos.entity.pos.EmployeeSalary;
import com.example.pos.repo.pos.EmployeeRepository;
import com.example.pos.repo.pos.EmployeeSalaryRepository;

import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class EmployeeSalaryService {

    private final EmployeeRepository employeeRepo;
    private final EmployeeSalaryRepository salaryRepo;

    public Status paySalary(SalaryPaymentRequestDto dto) {

        Employee emp = employeeRepo.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        boolean alreadyPaid = !salaryRepo
                .findByEmployeeIdAndPaymentTypeAndStatusAndIsActiveTrue(
                        emp.getId(),
                        "SALARY",
                        "PAID"
                )
                .isEmpty();


        if (alreadyPaid) {
            return new Status(
                    StatusMessage.FAILURE,
                    "Salary already paid for this month"
            );
        }

        BigDecimal baseSalary = emp.getSalary();
        BigDecimal bonus = dto.getBonus() != null ? dto.getBonus() : BigDecimal.ZERO;
        BigDecimal overtime = dto.getOverTime() != null ? dto.getOverTime() : BigDecimal.ZERO;
        BigDecimal deduction = dto.getDeduction() != null ? dto.getDeduction() : BigDecimal.ZERO;
        BigDecimal advanceAdjustment = dto.getAdvanceAdjustment() != null
                ? dto.getAdvanceAdjustment()
                : BigDecimal.ZERO;
        LocalDate salaryMonth = dto.getSalaryDate() != null
                ? dto.getSalaryDate().withDayOfMonth(1)
                : LocalDate.now().withDayOfMonth(1);
        BigDecimal currentAdvance = getCurrentAdvance(emp.getId());

        if (advanceAdjustment.compareTo(currentAdvance) > 0) {
            return new Status(StatusMessage.FAILURE,
                    "Advance adjustment exceeds pending advance");
        }



        BigDecimal totalPayable =
                baseSalary
                        .add(bonus)
                        .add(overtime)
                        .subtract(deduction)
                        .subtract(advanceAdjustment);

        BigDecimal remainingAdvance = currentAdvance.subtract(advanceAdjustment);

        EmployeeSalary salary = new EmployeeSalary();
        salary.setEmployeeId(emp.getId());
        salary.setBaseSalary(baseSalary);
        salary.setBonus(bonus);
        salary.setOvertime(overtime);
        salary.setDeduction(deduction);
        salary.setAdvanceAdjusted(advanceAdjustment);
        salary.setRemainingAdvance(remainingAdvance);
        salary.setTotalPaid(totalPayable);
        salary.setPaymentType("SALARY");
        salary.setStatus("PAID");
        salary.setSalaryType(dto.getSalaryType());
        salary.setSalaryMonth(salaryMonth);
        salary.setActive(true);
        salary.setSalaryDate(LocalDate.now());
        salary.setPaidOn(LocalDateTime.now());

        salaryRepo.save(salary);

        return new Status(StatusMessage.SUCCESS, salary);
    }

    public Status giveAdvance(AdvanceSalaryRequestDto dto) {
        Employee emp = employeeRepo.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Get current month ka paid salary record
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);

        // Check if salary already paid for this month
        Optional<EmployeeSalary> existingPaidSalary = salaryRepo.findByEmployeeIdAndSalaryMonthAndPaymentTypeAndIsActive(
                emp.getId(),
                monthStart,
                "SALARY",
                true
        );

        BigDecimal currentAdvance = getCurrentAdvance(emp.getId());
        BigDecimal newAdvance = currentAdvance.add(dto.getAmount());

        LocalDateTime salaryMonth = dto.getPaidOn() != null
                ? dto.getPaidOn()
                : LocalDateTime.now();

        // Agar salary already paid hai
        if (existingPaidSalary.isPresent()) {
            EmployeeSalary paidSalary = existingPaidSalary.get();

            // Null safe operations
            BigDecimal currentTotalPaid = paidSalary.getTotalPaid() != null ?
                    paidSalary.getTotalPaid() : BigDecimal.ZERO;
            BigDecimal currentAdvanceGiven = paidSalary.getAdvanceGiven() != null ?
                    paidSalary.getAdvanceGiven() : BigDecimal.ZERO;

            // 1. Update existing paid salary record
            paidSalary.setTotalPaid(currentTotalPaid.subtract(dto.getAmount()));
            paidSalary.setAdvanceGiven(currentAdvanceGiven.add(dto.getAmount()));
            paidSalary.setRemainingAdvance(newAdvance);

            // Save updated salary record
            salaryRepo.save(paidSalary);

            // 2. Create advance record separately
            EmployeeSalary advanceRecord = new EmployeeSalary();
            advanceRecord.setEmployeeId(emp.getId());
            advanceRecord.setAdvanceGiven(dto.getAmount());
            advanceRecord.setRemainingAdvance(newAdvance);
            advanceRecord.setPaymentType("ADVANCE");
            advanceRecord.setStatus("ADJUSTED"); // Mark as adjusted in salary
            advanceRecord.setPaidOn(salaryMonth);
            advanceRecord.setActive(true);
            advanceRecord.setSalaryMonth(monthStart);

            // Set default values for null fields
            if (advanceRecord.getAdvanceGiven() == null) {
                advanceRecord.setAdvanceGiven(BigDecimal.ZERO);
            }
            if (advanceRecord.getRemainingAdvance() == null) {
                advanceRecord.setRemainingAdvance(BigDecimal.ZERO);
            }

            salaryRepo.save(advanceRecord);

            return new Status(
                    StatusMessage.SUCCESS,
                    "Advance adjusted from paid salary. Total pending advance: " + newAdvance
            );
        }
        else {
            // Normal advance without salary payment
            EmployeeSalary salary = new EmployeeSalary();
            salary.setEmployeeId(emp.getId());
            salary.setAdvanceGiven(dto.getAmount());
            salary.setRemainingAdvance(newAdvance);
            salary.setPaymentType("ADVANCE");
            salary.setStatus("PAID");
            salary.setPaidOn(salaryMonth);
            salary.setActive(true);
            salary.setSalaryMonth(monthStart);

            // Set default values
            if (salary.getAdvanceGiven() == null) {
                salary.setAdvanceGiven(BigDecimal.ZERO);
            }
            if (salary.getRemainingAdvance() == null) {
                salary.setRemainingAdvance(BigDecimal.ZERO);
            }

            salaryRepo.save(salary);

            return new Status(
                    StatusMessage.SUCCESS,
                    "Advance given. Total pending advance: " + newAdvance
            );
        }
    }
    private BigDecimal getCurrentAdvance(Long employeeId) {
        return salaryRepo
                .findTopByEmployeeIdAndIsActiveOrderByIdDesc(employeeId,true)
                .map(EmployeeSalary::getRemainingAdvance)
                .orElse(BigDecimal.ZERO);
    }


    public Status getEmployeeSalaryInfo(String name, String designation) {

        Employee emp = employeeRepo.findByNameAndDesignationAndActiveTrue(name, designation);
        if (emp == null) {
            return new Status(StatusMessage.FAILURE, "Employee not found");
        }

        BigDecimal pendingAdvance = salaryRepo
                .findTopByEmployeeIdAndIsActiveOrderByIdDesc(emp.getId(),true)
                .map(EmployeeSalary::getRemainingAdvance)
                .orElse(BigDecimal.ZERO);

        EmployeeSalaryInfoDto dto = new EmployeeSalaryInfoDto();
        dto.setEmployeeId(emp.getId());
        dto.setName(emp.getName());
        dto.setDesignation(emp.getDesignation());
        dto.setBaseSalary(emp.getSalary());
        dto.setPendingAdvance(pendingAdvance);

        return new Status(StatusMessage.SUCCESS, dto);
    }

    public Status getEmployeeSalaryDetailView(LocalDate startDate, LocalDate endDate) {
        List<Employee> employees = employeeRepo.findByActiveTrue();
        List<EmployeeSalaryDetailViewDto> response = new ArrayList<>();

        for (Employee emp : employees) {
            // 1️⃣ Paid Salaries (only active)
            List<EmployeeSalary> salaries = salaryRepo.findByEmployeeIdAndPaymentTypeAndStatusAndIsActiveTrue(
                            emp.getId(),
                            "SALARY",
                            "PAID"
                    ).stream()
                    // ✅ Filter salaries by date range
                    .filter(s -> s.getSalaryDate() != null &&
                            !s.getSalaryMonth().isBefore(startDate) &&
                            !s.getSalaryMonth().isAfter(endDate))
                    .toList();

            if (salaries.isEmpty()) continue;

            // 2️⃣ All advances for this employee (PAID and ADJUSTED status)
            List<EmployeeSalary> allAdvances = salaryRepo.findByEmployeeIdAndPaymentTypeAndIsActiveTrueOrderByPaidOnAsc(
                            emp.getId(),
                            "ADVANCE"
                    ).stream()
                    // ✅ Filter advances by date range
                    .filter(a -> a.getPaidOn() != null &&
                            !a.getPaidOn().toLocalDate().isBefore(startDate) &&
                            !a.getPaidOn().toLocalDate().isAfter(endDate))
                    .toList();

            EmployeeSalaryDetailViewDto empDto = new EmployeeSalaryDetailViewDto();
            empDto.setEmployeeId(emp.getId());
            empDto.setEmployeeName(emp.getName());
            empDto.setDesignation(emp.getDesignation());

            List<EmployeeMonthlySalaryDetailDto> monthlyList = new ArrayList<>();

            for (EmployeeSalary salary : salaries) {
                YearMonth salaryMonth = YearMonth.from(salary.getSalaryDate());

                // Filter advances of the same month (both PAID and ADJUSTED)
                List<EmployeeSalary> monthAdvances = allAdvances.stream()
                        .filter(a -> a.getPaidOn() != null &&
                                YearMonth.from(a.getPaidOn().toLocalDate()).equals(salaryMonth))
                        .toList();

                EmployeeMonthlySalaryDetailDto monthDto = new EmployeeMonthlySalaryDetailDto();
                monthDto.setSalaryMonth(salary.getSalaryMonth());
                monthDto.setSalaryPaidOn(salary.getPaidOn() != null ? salary.getPaidOn() : salary.getSalaryDate().atStartOfDay());
                monthDto.setBaseSalary(emp.getSalary());

                // Calculate total advances (given in this month)
                BigDecimal totalAdvanceGiven = monthAdvances.stream()
                        .map(EmployeeSalary::getAdvanceGiven)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate total advance adjusted (from salary record)
                BigDecimal advanceAdjusted = nvl(salary.getAdvanceAdjusted());

                // Calculate remaining advance after adjustment
                BigDecimal remainingAdvanceAfterAdjustment = BigDecimal.ZERO;
                if (!monthAdvances.isEmpty()) {
                    // Get the latest advance record to find remaining advance
                    EmployeeSalary latestAdvance = monthAdvances.get(monthAdvances.size() - 1);
                    remainingAdvanceAfterAdjustment = nvl(latestAdvance.getRemainingAdvance());
                }

                monthDto.setTotalAdvancesCount(monthAdvances.size());
                monthDto.setTotalAdvanceAmount(totalAdvanceGiven);
                monthDto.setBonus(nvl(salary.getBonus()));
                monthDto.setOverTime(nvl(salary.getOvertime()));
                monthDto.setDeduction(nvl(salary.getDeduction()));
                monthDto.setAdvanceAdjustment(advanceAdjusted);

                // Add totalPaid field (actual amount paid after advance adjustment)
                monthDto.setTotalPaid(nvl(salary.getTotalPaid()));

                // Calculate net salary properly
                // Net Salary = Base + Bonus + Overtime - Deduction - Advance Adjusted
                BigDecimal netSalary = emp.getSalary()
                        .add(nvl(salary.getBonus()))
                        .add(nvl(salary.getOvertime()))
                        .subtract(nvl(salary.getDeduction()))
                        .subtract(advanceAdjusted);

                monthDto.setNetSalary(netSalary);

                // Also calculate gross salary
                BigDecimal grossSalary = emp.getSalary()
                        .add(nvl(salary.getBonus()))
                        .add(nvl(salary.getOvertime()));
                monthDto.setGrossSalary(grossSalary);

                // Calculate pending advance (advances given but not yet adjusted)
                BigDecimal pendingAdvance = totalAdvanceGiven.subtract(advanceAdjusted);
                if (pendingAdvance.compareTo(BigDecimal.ZERO) < 0) {
                    pendingAdvance = BigDecimal.ZERO;
                }
                monthDto.setPendingAdvance(pendingAdvance);

                List<AdvanceDetailDto> advanceDetails = new ArrayList<>();
                for (EmployeeSalary adv : monthAdvances) {
                    AdvanceDetailDto ad = new AdvanceDetailDto();
                    ad.setAdvanceDate(adv.getPaidOn()); // include exact time
                    ad.setAdvanceAmount(nvl(adv.getAdvanceGiven()));
                    ad.setRemainingAdvance(nvl(adv.getRemainingAdvance()));
                    ad.setStatus(adv.getStatus()); // Add status (PAID or ADJUSTED)
                    advanceDetails.add(ad);
                }

                monthDto.setAdvances(advanceDetails);
                monthlyList.add(monthDto);
            }

            empDto.setMonthlyDetails(monthlyList);
            response.add(empDto);
        }

        return new Status(StatusMessage.SUCCESS, response);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Transactional
    public Status deleteSalary(Long salaryId) {

        // 1️⃣ Get salary record
       List< EmployeeSalary> salary = salaryRepo.findByEmployeeIdAndIsActiveTrue(salaryId);

        if (salary == null) {
            return new Status(StatusMessage.FAILURE,"Record already deleted");
        }

        Long employeeId = salary.get(0).getEmployeeId();
        // agar direct employeeId hai to:
        // Long employeeId = salary.getEmployeeId();

        // 2️⃣ Soft delete ALL salaries of this employee
        List<EmployeeSalary> salaries =
                salaryRepo.findByEmployeeIdAndIsActiveTrue(employeeId);

        for (EmployeeSalary s : salaries) {
            s.setActive(false);
        }
        salaryRepo.saveAll(salaries);

        // 3️⃣ Soft delete ALL advances of this employee
        List<EmployeeSalary> advances =
                salaryRepo.findByEmployeeIdAndIsActiveTrue(employeeId);

        for (EmployeeSalary adv : advances) {
            adv.setActive(false);
        }
        salaryRepo.saveAll(advances);

        return new Status(StatusMessage.SUCCESS,"All salary and advance records deleted successfully");
    }



}
