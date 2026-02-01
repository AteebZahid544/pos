package com.example.pos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ProductionDashboardDto {

    private Long recordId;
    private String productName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalDurationInSeconds;
    private List<StepTimeDto> steps;
    private Integer quantity;
    private String employeeName;
    private String companyName;

@Data
@AllArgsConstructor
public static class StepTimeDto {
        private Long stepId;
        private String stepName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long durationInSeconds;

}}
