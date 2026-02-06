package com.example.pos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class UpdateEmployeeAuthoritiesRequest {
    
    private List<Integer> authorityIds;
}