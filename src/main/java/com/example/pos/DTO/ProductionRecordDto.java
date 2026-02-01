package com.example.pos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRecordDto {
    private Long id;
    private Long productId;
    private String productName;
    private List<StepTimeDto> steps;
    private Long totalElapsedSeconds;
    private LocalDateTime startTime;
    private String employeeName;
    private String status;

    // Constructor without startTime and employeeName
    public ProductionRecordDto(Long id, Long productId, String productName,
                               List<StepTimeDto> steps, Long totalElapsedSeconds, String status) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.steps = steps;
        this.totalElapsedSeconds = totalElapsedSeconds;
        this.status = status;
    }

    // Constructor without productName
    public ProductionRecordDto(Long id, Long productId, List<StepTimeDto> steps) {
        this.id = id;
        this.productId = productId;
        this.steps = steps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepTimeDto {
        private Long id;
        private String stepName;
        private Long durationInSeconds;
        private Long elapsedSeconds;
        private String status;

        // Constructor for backward compatibility (3 parameters)
        public StepTimeDto(Long id, String stepName, Long durationInSeconds) {
            this.id = id;
            this.stepName = stepName;
            this.durationInSeconds = durationInSeconds;
        }

        // Constructor with 5 parameters - FIXED: Change parameter order to avoid conflict
        public StepTimeDto(Long id, String stepName, Long durationInSeconds,
                           String status, Long elapsedSeconds) { // Changed order
            this.id = id;
            this.stepName = stepName;
            this.durationInSeconds = durationInSeconds;
            this.status = status;
            this.elapsedSeconds = elapsedSeconds;
        }
    }
}