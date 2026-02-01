package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "step_time")
public class StepTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name= "step_name")
    private String stepName;

    @Column(name= "step_order")
    private Integer stepOrder;

    @Column(name = "start_time", nullable = true)
    private LocalDateTime startTime;

    @Column(name = "end_time")

    private LocalDateTime endTime;

    @Column(name = "duration_in_seconds")

    private Long durationInSeconds;

    @Column(name = "elapsed_seconds") // Track elapsed time for this step
    private Long elapsedSeconds = 0L;

    @Column(name = "status") // Add status: NOT_STARTED, IN_PROGRESS, COMPLETED
    private String status = "NOT_STARTED";

    @ManyToOne
    @JoinColumn(name = "production_record_id")
    @JsonIgnore
    private ProductionRecord productionRecord;

    // Getters and setters
}
