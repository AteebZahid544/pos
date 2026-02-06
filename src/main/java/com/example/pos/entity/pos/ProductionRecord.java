package com.example.pos.entity.pos;

import jakarta.persistence.*;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "production_record")
public class ProductionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "employee_name")

    private String employeeName;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "start_time", nullable = false) // Make sure this is NOT NULL

    private LocalDateTime startTime;

    @Column(name = "end_time")

    private LocalDateTime endTime;


    @OneToMany(mappedBy = "productionRecord", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<StepTime> steps = new ArrayList<>();

    @Column(name = "user_id")
    private Long userId; // User who started the production

    @Column(name = "session_token")
    private String sessionToken; // Session token when started

    @Column(name = "last_updated_by")
    private Long lastUpdatedBy;

    @Column(name = "pause_time") // Add this field
    private LocalDateTime pauseTime;

    @Column(name = "status") // Add status field: ACTIVE, PAUSED, COMPLETED, CANCELLED
    private String status = "ACTIVE";

    @Column(name = "total_elapsed_seconds") // Track total active time
    private Long totalElapsedSeconds = 0L;

    public boolean isActive() {
        return "ACTIVE".equals(status) && endTime == null;
    }

    // Helper method to check if production is paused
    public boolean isPaused() {
        return "PAUSED".equals(status);
    }
}
