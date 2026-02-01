package com.example.pos.Controller;

import com.example.pos.DTO.ClientRequirementRequestDto;
import com.example.pos.DTO.UpdateRequirementDto;
import com.example.pos.Service.CompanyRequirementService;
import com.example.pos.entity.pos.ClientRequirement;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company-requirements")
public class CompanyRequirementController {

    @Autowired
    private CompanyRequirementService service;

    // 1️⃣ ADD company requirements
    @PostMapping
    public Status addDetails(@RequestBody ClientRequirementRequestDto dto) {
        return service.createRecord(dto);
    }

    // 2️⃣ GET company requirements (by company name)
    @GetMapping("/{companyName}")
    public List<ClientRequirement> getRecord(@PathVariable String companyName) {
        return service.getByCompanyName(companyName);
    }

    // 3️⃣ UPDATE requirements
    @PutMapping("/{id}")
    public Status updateRecord(
            @PathVariable Long id,
            @RequestBody UpdateRequirementDto dto) {
        return service.updateRequirement(id, dto.getRequirements());
    }

    @PutMapping("/updateContactByCompany")
    public Status updateContactByCompany(@RequestBody UpdateRequirementDto dto) {
        return service.updateContactInfoForCompany(dto);
    }


    // 4️⃣ DELETE
    @DeleteMapping("/{id}")
    public Status deleteRecord(@PathVariable Long id) {
       return service.deleteRecord(id);

    }

    @GetMapping("/companyNames")
    public Status getCompanyNames(){
        return service.getCompanyNames();
    }
}
