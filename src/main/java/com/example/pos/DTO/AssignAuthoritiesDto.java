package com.example.pos.DTO;

import lombok.Data;
import java.util.List;

@Data
public class AssignAuthoritiesDto {
    private Long employeeId;          // selected employee
    private List<Integer> authorityIds; // list of authorities to assign
}
