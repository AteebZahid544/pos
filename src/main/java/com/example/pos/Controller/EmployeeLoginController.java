package com.example.pos.Controller;

import com.example.pos.DTO.AssignAuthoritiesDto;
import com.example.pos.DTO.CreateEmployeeDto;
import com.example.pos.Service.EmployeeLoginService;
import com.example.pos.util.Status;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
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
}
