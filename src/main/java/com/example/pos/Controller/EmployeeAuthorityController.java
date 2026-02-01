package com.example.pos.Controller;


import com.example.pos.Service.EmployeeAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee/authority")
public class EmployeeAuthorityController {

    @Autowired
    private EmployeeAuthorityService empAuthService;

    // Assign authorities to an employee
    @PostMapping("/assign")
    public Map<String, String> assignAuthorities(@RequestBody Map<String, Object> request){
        String employeeId = (String) request.get("employeeId");
        List<Integer> authorityIds = (List<Integer>) request.get("authorityIds");

        empAuthService.assignAuthorities(employeeId, authorityIds);

        return Map.of("status", "success", "message", "Authorities assigned successfully");
    }

    // Get authorities of an employee
    @GetMapping("/{employeeId}")
    public Map<String, Object> getAuthorities(@PathVariable String employeeId){
        List<String> authorities = empAuthService.getAuthorities(employeeId);
        return Map.of("employeeId", employeeId, "authorities", authorities);
    }
}
