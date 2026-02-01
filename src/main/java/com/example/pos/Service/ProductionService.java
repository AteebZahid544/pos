package com.example.pos.Service;


import com.example.pos.DTO.ProductionDashboardDto;
import com.example.pos.DTO.ProductionRecordDto;
import com.example.pos.DTO.StepTimeResponseDto;
import com.example.pos.entity.pos.ProductManufacture;

import com.example.pos.entity.pos.ProductStep;
import com.example.pos.entity.pos.ProductionRecord;
import com.example.pos.entity.pos.StepTime;
import com.example.pos.repo.central.SessionRepo;
import com.example.pos.repo.pos.ProductStepRepository;
import com.example.pos.repo.pos.ProductionRecordRepository;
import com.example.pos.repo.pos.ProductionRepository;
import com.example.pos.repo.pos.StepTimeRepository;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductionService {
    private final ProductionRepository productRepo;
    private final ProductStepRepository stepRepo;
    private final ProductionRecordRepository recordRepo;
    private final StepTimeRepository stepTimeRepo;
    private final SessionRepo sessionRepo;
    private final HttpServletRequest request;

    public ProductionService(ProductionRepository productRepo, ProductStepRepository stepRepo,
                             ProductionRecordRepository recordRepo, StepTimeRepository stepTimeRepo,
                             SessionRepo sessionRepo, HttpServletRequest request) {
        this.productRepo = productRepo;
        this.stepRepo = stepRepo;
        this.recordRepo = recordRepo;
        this.stepTimeRepo = stepTimeRepo;
        this.sessionRepo = sessionRepo;
        this.request = request;
    }

    private Long getCurrentUserId() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return sessionRepo.findByToken(token)
                    .map(session -> 1L) // Placeholder
                    .orElse(1L);
        }
        return 1L;
    }

    private String getCurrentSessionToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }

    private String getCurrentUsername() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return sessionRepo.findByToken(token)
                    .map(session -> session.getPhoneNumber())
                    .orElse("Unknown User");
        }
        return "Unknown User";
    }

    @Transactional
    public ProductManufacture addProductWithSteps(String productName, List<String> steps) {
        ProductManufacture product = new ProductManufacture();
        product.setProductName(productName);

        List<ProductStep> stepEntities = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            ProductStep ps = new ProductStep();
            ps.setStepName(steps.get(i));
            ps.setStepOrder(i + 1);
            ps.setProduct(product);
            stepEntities.add(ps);
        }
        product.setSteps(stepEntities);

        return productRepo.save(product);
    }

    @Transactional
    public ProductionRecordDto startProduction(Long productId) {
        Long userId = getCurrentUserId();
        String username = getCurrentUsername();

        // Get product with steps eagerly
        ProductManufacture product = getProductWithSteps(productId);

        ProductionRecord record = new ProductionRecord();
        record.setProductId(productId);
        record.setProductName(product.getProductName());
        record.setStartTime(LocalDateTime.now());
        record.setUserId(userId);
        record.setLastUpdatedBy(userId);
        record.setEmployeeName(username);
        record.setStatus("ACTIVE");

        // Don't create StepTime records here - create them when step starts
        record.setSteps(new ArrayList<>());

        recordRepo.save(record);
        return mapToDto(record);
    }

    @Transactional
    public StepTimeResponseDto startStep(Long productionRecordId, String stepName) {
        ProductionRecord record = recordRepo.findByIdWithSteps(productionRecordId)
                .orElseThrow(() -> new RuntimeException("Production record not found"));

        // Check if production is completed or cancelled
        if ("COMPLETED".equals(record.getStatus()) || "CANCELLED".equals(record.getStatus())) {
            throw new RuntimeException("This production is already completed or cancelled");
        }

        // If production was paused, resume it
        if ("PAUSED".equals(record.getStatus())) {
            record.setStatus("ACTIVE");
            if (record.getPauseTime() != null) {
                // Calculate elapsed time during pause
                Duration pauseDuration = Duration.between(record.getPauseTime(), LocalDateTime.now());
                record.setTotalElapsedSeconds(record.getTotalElapsedSeconds() + pauseDuration.getSeconds());
                record.setPauseTime(null);
            }
        }

        // Check if step already exists and is in progress
        Optional<StepTime> existingStep = record.getSteps().stream()
                .filter(s -> s.getStepName().equals(stepName))
                .findFirst();

        StepTime stepTime;
        if (existingStep.isPresent()) {
            stepTime = existingStep.get();
            if ("IN_PROGRESS".equals(stepTime.getStatus())) {
                throw new RuntimeException("This step is already in progress");
            }
            // If step was paused, resume it
            stepTime.setStartTime(LocalDateTime.now());
            stepTime.setStatus("IN_PROGRESS");
        } else {
            // Create new step
            stepTime = new StepTime();
            stepTime.setStepName(stepName);
            stepTime.setProductionRecord(record);
            stepTime.setStartTime(LocalDateTime.now());
            stepTime.setStatus("IN_PROGRESS");

            // Find step order
            ProductManufacture product = getProductWithSteps(record.getProductId());
            product.getSteps().stream()
                    .filter(s -> s.getStepName().equals(stepName))
                    .findFirst()
                    .ifPresent(s -> stepTime.setStepOrder(s.getStepOrder()));

            record.getSteps().add(stepTime);
        }

        stepTimeRepo.save(stepTime);
        record.setLastUpdatedBy(getCurrentUserId());
        recordRepo.save(record);

        return new StepTimeResponseDto(stepTime.getId(), stepTime.getStepName(),
                stepTime.getStartTime(), stepTime.getElapsedSeconds());
    }

    @Transactional
    public StepTimeResponseDto pauseStep(Long stepId) {
        StepTime step = stepTimeRepo.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));

        if (!"IN_PROGRESS".equals(step.getStatus())) {
            throw new RuntimeException("Step is not in progress");
        }

        // Calculate elapsed time for this step
        if (step.getStartTime() != null) {
            Duration elapsed = Duration.between(step.getStartTime(), LocalDateTime.now());
            step.setElapsedSeconds(step.getElapsedSeconds() + elapsed.getSeconds());
        }

        step.setStatus("PAUSED");
        step.setStartTime(null); // Clear start time since step is paused
        stepTimeRepo.save(step);

        // Also pause the production if all steps are paused
        ProductionRecord record = step.getProductionRecord();
        boolean allStepsPausedOrCompleted = record.getSteps().stream()
                .allMatch(s -> "PAUSED".equals(s.getStatus()) || "COMPLETED".equals(s.getStatus()));

        if (allStepsPausedOrCompleted && "ACTIVE".equals(record.getStatus())) {
            record.setStatus("PAUSED");
            record.setPauseTime(LocalDateTime.now());
            recordRepo.save(record);
        }

        return new StepTimeResponseDto(step.getId(), step.getStepName(),
                null, step.getElapsedSeconds());
    }

    @Transactional
    public StepTimeResponseDto finishStep(Long stepId) {
        StepTime step = stepTimeRepo.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));

        // Calculate final duration
        long finalDuration = step.getElapsedSeconds();
        if (step.getStartTime() != null && "IN_PROGRESS".equals(step.getStatus())) {
            Duration additional = Duration.between(step.getStartTime(), LocalDateTime.now());
            finalDuration += additional.getSeconds();
        }

        step.setEndTime(LocalDateTime.now());
        step.setDurationInSeconds(finalDuration);
        step.setStatus("COMPLETED");
        stepTimeRepo.save(step);

        // Update production total elapsed time
        ProductionRecord record = step.getProductionRecord();
        record.setTotalElapsedSeconds(record.getTotalElapsedSeconds() + finalDuration);
        record.setLastUpdatedBy(getCurrentUserId());
        recordRepo.save(record);

        return new StepTimeResponseDto(step.getId(), step.getStepName(),
                step.getStartTime(), finalDuration);
    }

    @Transactional
    public ProductionRecordDto pauseProduction(Long productionRecordId) {
        ProductionRecord record = recordRepo.findByIdWithSteps(productionRecordId)
                .orElseThrow(() -> new RuntimeException("Production record not found"));

        if (!"ACTIVE".equals(record.getStatus())) {
            throw new RuntimeException("Production is not active");
        }

        // Pause all in-progress steps
        for (StepTime step : record.getSteps()) {
            if ("IN_PROGRESS".equals(step.getStatus())) {
                if (step.getStartTime() != null) {
                    Duration elapsed = Duration.between(step.getStartTime(), LocalDateTime.now());
                    step.setElapsedSeconds(step.getElapsedSeconds() + elapsed.getSeconds());
                    step.setStartTime(null);
                }
                step.setStatus("PAUSED");
                stepTimeRepo.save(step);
            }
        }

        record.setStatus("PAUSED");
        record.setPauseTime(LocalDateTime.now());
        record.setLastUpdatedBy(getCurrentUserId());
        recordRepo.save(record);

        return mapToDto(record);
    }

    @Transactional
    public ProductionRecordDto resumeProduction(Long productionRecordId) {
        ProductionRecord record = recordRepo.findByIdWithSteps(productionRecordId)
                .orElseThrow(() -> new RuntimeException("Production record not found"));

        // Check if production is completed or cancelled
        if ("COMPLETED".equals(record.getStatus()) || "CANCELLED".equals(record.getStatus())) {
            throw new RuntimeException("This production is already completed or cancelled");
        }

        // Allow resuming from both ACTIVE and PAUSED status
        if (!"ACTIVE".equals(record.getStatus()) && !"PAUSED".equals(record.getStatus())) {
            throw new RuntimeException("Production cannot be resumed from status: " + record.getStatus());
        }

        // Calculate pause duration if production was paused
        if ("PAUSED".equals(record.getStatus())) {
            if (record.getPauseTime() != null) {
                // Calculate how long the production was paused
                Duration pauseDuration = Duration.between(record.getPauseTime(), LocalDateTime.now());
                record.setTotalElapsedSeconds(record.getTotalElapsedSeconds() + pauseDuration.getSeconds());
                record.setPauseTime(null);
            }
        }

        // Resume any paused steps
        for (StepTime step : record.getSteps()) {
            if ("PAUSED".equals(step.getStatus()) && step.getEndTime() == null) {
                // IMPORTANT: Set status to IN_PROGRESS but DON'T reset startTime
                // The step should continue timing from where it left off
                step.setStatus("IN_PROGRESS");
                step.setStartTime(LocalDateTime.now()); // Set new start time
                stepTimeRepo.save(step);

                // IMPORTANT: We need to calculate the offset
                // If the step already had elapsedSeconds from before, we'll use that
                // The timer will now continue from elapsedSeconds + new duration
            }
        }

        record.setStatus("ACTIVE");
        record.setSessionToken(getCurrentSessionToken());
        record.setLastUpdatedBy(getCurrentUserId());
        recordRepo.save(record);

        return mapToDto(record);
    }
    @Transactional
    public ProductionRecordDto finishProduction(Long productionRecordId, String employeeName,
                                                Integer quantity, String companyName) {
        ProductionRecord record = recordRepo.findByIdWithSteps(productionRecordId)
                .orElseThrow(() -> new RuntimeException("Production record not found"));

        // Finish any in-progress steps
        for (StepTime step : record.getSteps()) {
            if ("IN_PROGRESS".equals(step.getStatus())) {
                if (step.getStartTime() != null) {
                    Duration elapsed = Duration.between(step.getStartTime(), LocalDateTime.now());
                    step.setElapsedSeconds(step.getElapsedSeconds() + elapsed.getSeconds());
                    step.setDurationInSeconds(step.getElapsedSeconds());
                }
                step.setEndTime(LocalDateTime.now());
                step.setStatus("COMPLETED");
                stepTimeRepo.save(step);
            }
        }

        // Calculate final total elapsed time
        if (record.getStatus().equals("ACTIVE")) {
            record.setTotalElapsedSeconds(record.getTotalElapsedSeconds() +
                    Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds());
        }

        record.setEndTime(LocalDateTime.now());
        record.setEmployeeName(employeeName);
        record.setTotalQuantity(quantity);
        record.setCompanyName(companyName);
        record.setStatus("COMPLETED");
        record.setLastUpdatedBy(getCurrentUserId());
        recordRepo.save(record);

        return mapToDto(record);
    }

    @Transactional
    public ProductionRecordDto cancelProduction(Long productionRecordId) {
        ProductionRecord record = recordRepo.findByIdWithSteps(productionRecordId)
                .orElseThrow(() -> new RuntimeException("Production record not found"));

        record.setEndTime(LocalDateTime.now());
        record.setStatus("CANCELLED");
        record.setLastUpdatedBy(getCurrentUserId());
        recordRepo.save(record);

        return mapToDto(record);
    }

    @Transactional(readOnly = true)
    public ProductionRecordDto getProductionWithElapsedTime(Long productionRecordId) {
        ProductionRecord record = recordRepo.findByIdWithSteps(productionRecordId)
                .orElseThrow(() -> new RuntimeException("Production record not found"));

        // Calculate current elapsed time if active
        long currentElapsed = record.getTotalElapsedSeconds();
        if ("ACTIVE".equals(record.getStatus())) {
            LocalDateTime fromTime = record.getPauseTime() != null ? record.getPauseTime() : record.getStartTime();
            currentElapsed += Duration.between(fromTime, LocalDateTime.now()).getSeconds();
        }

        // Create DTO with elapsed time
        ProductionRecordDto dto = mapToDto(record);
        dto.setTotalElapsedSeconds(currentElapsed);

        // Calculate step elapsed times
        for (ProductionRecordDto.StepTimeDto stepDto : dto.getSteps()) {
            Optional<StepTime> step = record.getSteps().stream()
                    .filter(s -> s.getId().equals(stepDto.getId()))
                    .findFirst();

            if (step.isPresent()) {
                StepTime stepTime = step.get();
                long stepElapsed = stepTime.getElapsedSeconds();
                if ("IN_PROGRESS".equals(stepTime.getStatus()) && stepTime.getStartTime() != null) {
                    stepElapsed += Duration.between(stepTime.getStartTime(), LocalDateTime.now()).getSeconds();
                }
                stepDto.setElapsedSeconds(stepElapsed);
                stepDto.setStatus(stepTime.getStatus());
            }
        }

        return dto;
    }
    // Helper methods
    @Transactional(readOnly = true)
    public ProductManufacture getProductWithSteps(Long productId) {
        return productRepo.findByIdWithSteps(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }

    @Transactional(readOnly = true)
    public List<ProductManufacture> getAllProducts() {
        return productRepo.findAll();
    }

    private ProductionRecordDto mapToDto(ProductionRecord record) {
        List<ProductionRecordDto.StepTimeDto> stepDtos = record.getSteps().stream()
                .map(s -> new ProductionRecordDto.StepTimeDto(
                        s.getId(),
                        s.getStepName(),
                        s.getDurationInSeconds(),
                        s.getStatus(), // status comes before elapsedSeconds
                        s.getElapsedSeconds()
                ))
                .toList();

        return new ProductionRecordDto(
                record.getId(),
                record.getProductId(),
                record.getProductName(),
                stepDtos,
                record.getTotalElapsedSeconds(),
                record.getStartTime(),
                record.getEmployeeName(),
                record.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getUserResumableProductions() {
        Long userId = getCurrentUserId();

        // Use the repository method
        List<ProductionRecord> records = recordRepo.findResumableProductions(userId);

        return records.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getUserActiveProductions() {
        Long userId = getCurrentUserId();
        // Use the method that eagerly fetches steps
        List<ProductionRecord> records = recordRepo.findByUserIdAndStatusAndEndTimeIsNull(userId, "ACTIVE");
        return records.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getPausedProductions() {
        Long userId = getCurrentUserId();
        // Use the method that eagerly fetches steps
        List<ProductionRecord> records = recordRepo.findByUserIdAndStatus(userId, "PAUSED");
        return records.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getActiveProductions() {
        // Use the method that eagerly fetches steps
        List<ProductionRecord> records = recordRepo.findByStatusAndEndTimeIsNull("ACTIVE");
        return records.stream()
                .map(this::mapToDto)
                .toList();
    }

    // Add this new method for getting all resumable productions
    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getResumableProductions() {
        Long userId = getCurrentUserId();
        List<ProductionRecord> records = recordRepo.findResumableProductions(userId);
        return records.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionDashboardDto> getDailyProduction(LocalDate date) {
        List<ProductionRecord> records = recordRepo.findByDateWithSteps(date);

        return records.stream().map(record -> {
            ProductManufacture product = getProductWithSteps(record.getProductId());

            long totalDuration = record.getSteps().stream()
                    .mapToLong(s -> s.getDurationInSeconds() != null ? s.getDurationInSeconds() : 0)
                    .sum();

            List<ProductionDashboardDto.StepTimeDto> steps = record.getSteps().stream()
                    .map(s -> new ProductionDashboardDto.StepTimeDto(
                            s.getId(),
                            s.getStepName(),
                            s.getStartTime(),
                            s.getEndTime(),
                            s.getDurationInSeconds()
                    ))
                    .toList();

            return new ProductionDashboardDto(
                    record.getId(),
                    product != null ? product.getProductName() : "N/A",
                    record.getStartTime(),
                    record.getEndTime(),
                    totalDuration,
                    steps,
                    record.getTotalQuantity(),
                    record.getEmployeeName(),
                    record.getCompanyName());
        }).toList();
    }

}