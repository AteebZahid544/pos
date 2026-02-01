package com.example.pos.Service;

import com.example.pos.DTO.EmployeeRequestDto;
import com.example.pos.DTO.EmployeeResponseDto;

import com.example.pos.entity.pos.Employee;
import com.example.pos.repo.pos.EmployeeRepository;
import com.example.pos.util.SalaryType;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepo;

    @Value("${invoice.upload.path}")
    private String invoiceUploadPath;

    public Status create(
            String name,
            String address,
            String contactNumber,
            String designation,
            BigDecimal salary,
            SalaryType salaryType
    ) {

        Employee emp = employeeRepo.findByNameAndAddressAndActive(name, address, true);
        if (Objects.nonNull(emp)) {
            return new Status(StatusMessage.FAILURE, "This employee record already exists");
        }

        Employee employee = new Employee();
        employee.setName(name);
        employee.setAddress(address);
        employee.setDesignation(designation);
        employee.setContactNumber(contactNumber);
        employee.setSalary(salary);
        employee.setSalaryType(salaryType); // 🔥 VERY IMPORTANT
        employee.setActive(true);

        employeeRepo.save(employee);

        return map(employee);
    }

    public Status update(Long id, EmployeeRequestDto dto) {

        Employee emp = employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        emp.setName(dto.getName());
        emp.setAddress(dto.getAddress());
        emp.setDesignation(dto.getDesignation());
        emp.setContactNumber(dto.getContactNumber());
        emp.setSalary(dto.getSalary());
        emp.setSalaryType(dto.getSalaryType());


        employeeRepo.save(emp);
        return map(emp);
    }

    public Status delete(Long id) {
        Optional<Employee> employee = employeeRepo.findById(id);
        if (employee.isEmpty()){
            return new Status(StatusMessage.FAILURE,"Record already deleted");
        }
        Employee employee1= employee.get();
        employee1.setActive(false);

        employeeRepo.save(employee1);

        return new Status(StatusMessage.SUCCESS,"Record deleted successfully");
    }

    public Status getAll() {
        List<Employee> employees = employeeRepo.findByActiveTrueOrderByCreatedAtDesc();

        // Map to DTOs
        List<EmployeeResponseDto> responseDtos = employees.stream()
                .map(emp -> {
                    EmployeeResponseDto dto = new EmployeeResponseDto();
                    dto.setId(emp.getId());
                    dto.setName(emp.getName());
                    dto.setDesignation(emp.getDesignation());
                    dto.setAddress(emp.getAddress());
                    dto.setContactNumber(emp.getContactNumber());
                    dto.setSalary(emp.getSalary());
                    dto.setSalaryType(emp.getSalaryType());

                    if (emp.getIdCardImage() != null && !emp.getIdCardImage().isBlank()) {
                        String baseUrl = "http://localhost:8081/pos/api/employees/id-image?idPath=";
                        dto.setIdCardImage(baseUrl + URLEncoder.encode(emp.getIdCardImage(), StandardCharsets.UTF_8));
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return new Status(StatusMessage.SUCCESS, responseDtos);
    }


    public Employee getById(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id " + id));
    }


    private Status map(Employee emp) {
        EmployeeResponseDto dto = new EmployeeResponseDto();
        dto.setId(emp.getId());
        dto.setName(emp.getName());
        dto.setAddress(emp.getAddress());
        dto.setDesignation(emp.getDesignation());
        dto.setContactNumber(emp.getContactNumber());
        dto.setSalary(emp.getSalary());
        dto.setIdCardImage(emp.getIdCardImage());
        return new Status(StatusMessage.SUCCESS,dto);
    }

    public void saveInvoiceImage(Long id, MultipartFile file) throws IOException {

        // Base folder (from your config)
        File baseDir = new File(invoiceUploadPath);

        // Create a subfolder by status (e.g., D:/pos/invoices/purchase)
        File statusDir = new File(baseDir, "Id Pics");
        if (!statusDir.exists()) {
            boolean created = statusDir.mkdirs(); // creates all missing directories
            if (!created) {
                throw new IOException("Failed to create directory: " + statusDir.getAbsolutePath());
            }
        }

        // Build unique file name
        String fileName = "ID_" + id + "_" + System.currentTimeMillis()
                + "_" + file.getOriginalFilename();

        // Full path to save the file
        File destinationFile = new File(statusDir, fileName);

        // Copy file
        Files.copy(file.getInputStream(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Save file path to DB
        Employee invoice = employeeRepo
                .findById(id)
                .orElseThrow(() -> new RuntimeException("id not found"));

        invoice.setIdCardImage(destinationFile.getAbsolutePath());
        employeeRepo.save(invoice);
    }

}
