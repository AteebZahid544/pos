package com.example.pos.Service;

import com.example.pos.entity.central.EmployeeAuthority;
import com.example.pos.repo.central.EmployeeAuthorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeAuthorityService {

    @Autowired
    private EmployeeAuthorityRepository empAuthRepo;



    // Assign authorities to an employee
    public void assignAuthorities(String employeeId, List<Integer> authorityIds){
        // Remove old authorities first
        empAuthRepo.deleteByEmployeeId(employeeId);

        // Add new authorities
        authorityIds.forEach(authId -> {
            EmployeeAuthority ea = new EmployeeAuthority(employeeId, authId);
            empAuthRepo.save(ea);
        });
    }

    // Get all authorities assigned to an employee
    public List<String> getAuthorities(String employeeId){
        return empAuthRepo.findAuthoritiesByEmployeeId(employeeId);
    }
}
