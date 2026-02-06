package com.example.pos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class DeleteAuthoritiesRequest {

    private List<Integer> authorityIds;
}