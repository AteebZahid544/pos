package com.example.pos.Controller;

import com.example.pos.DTO.AssignAuthoritiesDto;
import com.example.pos.DTO.CreateEmployeeDto;
import com.example.pos.DTO.DeleteAuthoritiesRequest;
import com.example.pos.DTO.UpdateEmployeeAuthoritiesRequest;
import com.example.pos.Service.EmployeeLoginService;
import com.example.pos.util.Status;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/employee-login")
public class EmployeeLoginController {

    @Autowired

    private EmployeeLoginService employeeLoginService;

    @PostMapping("/owner/employee")

    private Status createEmployee(@RequestBody CreateEmployeeDto dto){
        return employeeLoginService.createEmployee(dto);
    }

    @GetMapping("/getEmployees")
    private Status getEmployees(){
        return employeeLoginService.getEmployees();
    }

    @GetMapping("/getAuthorities")
    private Status getAuthorities(){
        return employeeLoginService.getAuthorities();
    }
    @PostMapping("/assign-authorities")
    public Status assignAuthorities(@RequestBody AssignAuthoritiesDto dto) {
        return employeeLoginService.assignAuthorities(dto);
    }

    @GetMapping("/getUserNameAndAuthorities")
    public Status getUsernameAndAuthorities(){
        return employeeLoginService.getEmployeesAndAuthorities();
    }

    @PutMapping("/employee/{employeeId}")
    public Status updateEmployeeAuthorities(
            @PathVariable String employeeId,
             @RequestBody UpdateEmployeeAuthoritiesRequest request) {

        log.info("Request received to update authorities for employee ID: {} with authorities: {}",
                employeeId, request.getAuthorityIds());

        return employeeLoginService.updateEmployeeAuthorities(
                employeeId, request.getAuthorityIds());

    }

    // 5. Delete specific authorities for an employee
    @DeleteMapping("/employee/{employeeId}/specific")
    public Status deleteSpecificEmployeeAuthorities(
            @PathVariable String employeeId,
            @RequestBody DeleteAuthoritiesRequest request) {

        log.info("Request received to delete specific authorities for employee ID: {} with authorities: {}",
                employeeId, request.getAuthorityIds());

        return employeeLoginService.deleteSpecificEmployeeAuthorities(
                employeeId, request.getAuthorityIds());

    }

    @DeleteMapping("/delete-login/{phoneNumber}")
    public Status deleteLogin(@PathVariable String phoneNumber){
        return employeeLoginService.deleteEmployeeUser(phoneNumber);
    }

}
