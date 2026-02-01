package com.example.pos.repo.pos;

import com.example.pos.entity.pos.ClientRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRequirementRepository 
        extends JpaRepository<ClientRequirement, Long> {

    List<ClientRequirement> findByCompanyNameIgnoreCaseAndActiveTrue(String companyName);
    ClientRequirement findByIdAndActiveTrue(Long id);

    @Query("SELECT DISTINCT cr.companyName FROM ClientRequirement cr WHERE cr.active = true")
    List<String> findAllActiveCompanyNames();

    List<ClientRequirement>findByCompanyNameAndActiveTrue(String companyName);
}
