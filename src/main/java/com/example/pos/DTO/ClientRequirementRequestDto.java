package com.example.pos.DTO;

import lombok.Data;

import java.util.List;

@Data

public class ClientRequirementRequestDto {

    private String companyName;
    private String contactPerson;
    private String contactNumber;
    private List<String> requirements;
    private String updateRequirements;


}
