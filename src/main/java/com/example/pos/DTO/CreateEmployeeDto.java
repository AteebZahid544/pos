package com.example.pos.DTO;

import lombok.Data;

@Data
public class CreateEmployeeDto {
    private String username;
    private String password;
    private String phoneNumber;
}
