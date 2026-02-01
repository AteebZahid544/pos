package com.example.pos.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class StepTimeResponseDto { // Changed name to avoid conflict
    private Long id;
    private String stepName;
    private LocalDateTime startTime;
    private Long durationInSeconds;

    // Constructor for 3 parameters
    public StepTimeResponseDto(Long id, String stepName, Long durationInSeconds) {
        this.id = id;
        this.stepName = stepName;
        this.durationInSeconds = durationInSeconds;
    }

    // Constructor for 4 parameters (including startTime)
    public StepTimeResponseDto(Long id, String stepName, LocalDateTime startTime, Long durationInSeconds) {
        this.id = id;
        this.stepName = stepName;
        this.startTime = startTime;
        this.durationInSeconds = durationInSeconds;
    }
}