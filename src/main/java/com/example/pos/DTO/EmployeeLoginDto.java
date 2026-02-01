package com.example.pos.DTO;

import lombok.Data;

@Data
public class EmployeeLoginDto {
    private String phoneNumber;
    private String password; // owner-sent password
}
