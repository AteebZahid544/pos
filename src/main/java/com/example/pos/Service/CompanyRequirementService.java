package com.example.pos.Service;

import com.example.pos.DTO.ClientRequirementRequestDto;
import com.example.pos.DTO.UpdateRequirementDto;
import com.example.pos.entity.pos.ClientRequirement;
import com.example.pos.repo.pos.CompanyRequirementRepository;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CompanyRequirementService {

    @Autowired
    private CompanyRequirementRepository repo;


    // 1️⃣ CREATE
    public Status createRecord(ClientRequirementRequestDto dto) {
        for (String req : dto.getRequirements()) {

            ClientRequirement cr = new ClientRequirement();
            cr.setCompanyName(dto.getCompanyName());
            cr.setContactPerson(dto.getContactPerson());
            cr.setContactNumber(dto.getContactNumber());
            cr.setRequirements(req);
            cr.setActive(true);

            repo.save(cr);
        }

        return new Status(
                StatusMessage.SUCCESS,
                "Requirements saved successfully"
        );
    }

    // 2️⃣ READ (by company name)
    public List<ClientRequirement> getByCompanyName(String companyName) {
        return repo.findByCompanyNameIgnoreCaseAndActiveTrue(companyName);
    }

    // 3️⃣ UPDATE
    public Status updateRequirement(Long id, String requirement) {
        ClientRequirement cr = repo.findByIdAndActiveTrue(id);
        cr.setRequirements(requirement);
        repo.save(cr);
        return new Status(StatusMessage.SUCCESS, "Requirement Updated Successfully");
    }

    public Status updateContactInfoForCompany(UpdateRequirementDto dto) {
        // Find all active rows with the company name
        List<ClientRequirement> list = repo.findByCompanyNameAndActiveTrue(dto.getCompanyName());

        if (list.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No records found for this company");
        }

        // Update contact info for all rows
        for (ClientRequirement cr : list) {
            if (dto.getCompanyName() != null) cr.setCompanyName(dto.getCompanyName());
            if (dto.getContactPerson() != null) cr.setContactPerson(dto.getContactPerson());
            if (dto.getContactNumber() != null) cr.setContactNumber(dto.getContactNumber());
        }

        repo.saveAll(list);

        return new Status(StatusMessage.SUCCESS, "Contact info updated for all company records");
    }


    // 4️⃣ DELETE
    public Status deleteRecord(Long id) {
        Optional<ClientRequirement> clientRequirement = repo.findById(id);
        if (clientRequirement.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Record already deleted");
        }
        ClientRequirement clientRequirement1 = clientRequirement.get();
        clientRequirement1.setActive(false);
        repo.save(clientRequirement1);
        return new Status(StatusMessage.SUCCESS, "Record deleted successfully");
    }

    public Status getCompanyNames() {
        List<String> companyNames = repo.findAllActiveCompanyNames();

        if (companyNames == null || companyNames.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No company names found");
        }

        return new Status(StatusMessage.SUCCESS, companyNames);
    }

}
