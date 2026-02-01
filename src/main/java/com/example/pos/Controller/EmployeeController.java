package com.example.pos.Controller;

import com.example.pos.DTO.EmployeeRequestDto;

import com.example.pos.DTO.EmployeeResponseDto;
import com.example.pos.Service.EmployeeService;
import com.example.pos.entity.pos.Employee;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Status createEmployees(
            @RequestParam("request") String requestJson,
            @RequestParam(value = "IdImage", required = false) List<MultipartFile> idImages
    ) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        // 🔹 Parse JSON array
        List<EmployeeRequestDto> requests = mapper.readValue(
                requestJson,
                new TypeReference<List<EmployeeRequestDto>>() {}
        );

        List<EmployeeResponseDto> createdEmployees = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {

            EmployeeRequestDto req = requests.get(i);

            Status response = employeeService.create(
                    req.getName(),
                    req.getAddress(),
                    req.getContactNumber(),
                    req.getDesignation(),
                    req.getSalary(),
                    req.getSalaryType()
            );

            if (response.getCode() != StatusMessage.SUCCESS.getId()) {
                return response; // ❌ stop if any record fails
            }

            EmployeeResponseDto dto =
                    (EmployeeResponseDto) response.getAdditionalDetail();

            // 🔹 Save image if exists
            if (idImages != null && idImages.size() > i && !idImages.get(i).isEmpty()) {
                employeeService.saveInvoiceImage(dto.getId(), idImages.get(i));

                Employee updated = employeeService.getById(dto.getId());
                dto.setIdCardImage(updated.getIdCardImage());
            }

            createdEmployees.add(dto);
        }

        return new Status(StatusMessage.SUCCESS, "Employ record added successfully");
    }

    @PutMapping("/{id}")
    public Status updateEmployee(
            @PathVariable Long id,
            @RequestBody EmployeeRequestDto dto
    ) {

        return employeeService.update(id, dto);
    }

    @GetMapping
    public Status getAllEmployees() {
        return employeeService.getAll();
    }

    @DeleteMapping("/{id}")
    public Status deleteEmployee(@PathVariable Long id) {
        return employeeService.delete(id);
    }

    @GetMapping("/id-image")
    public ResponseEntity<Resource> getIdImage(@RequestParam String idPath) throws IOException {
        // Decode URL-encoded path
        String decodedPath = URLDecoder.decode(idPath, StandardCharsets.UTF_8);

        File file = new File(decodedPath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        // Determine content type
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

}
