package com.example.pos.DTO;

import lombok.Data;

@Data
public class ClientRequirementResponseDto {

    private Long id;
    private String companyName;
    private String contactPerson;
    private String contactNumber;
    private String email;
    private String requirements;
}
