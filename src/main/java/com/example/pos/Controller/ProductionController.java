package com.example.pos.Controller;

import com.example.pos.DTO.ProductionDashboardDto;
import com.example.pos.DTO.ProductionRecordDto;

import com.example.pos.DTO.StepTimeResponseDto;
import com.example.pos.Service.ProductionService;
import com.example.pos.entity.pos.ProductManufacture;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/production")
public class ProductionController {
    private final ProductionService service;

    public ProductionController(ProductionService service) {
        this.service = service;
    }

    // Add product with steps
    @PostMapping("/add-product")
    public ProductManufacture addProduct(@RequestParam String productName, @RequestBody List<String> steps) {
        return service.addProductWithSteps(productName, steps);
    }

    // Get steps for product - MUST be transactional
    @GetMapping("/steps/{productId}")
    @Transactional(readOnly = true) // Add this
    public List<StepDto> getSteps(@PathVariable Long productId) {
        ProductManufacture product = service.getProductWithSteps(productId);
        return product.getSteps().stream()
                .map(s -> new StepDto(s.getId(), s.getStepName(), s.getStepOrder()))
                .toList();
    }

    // DTO for steps
    public static class StepDto {
        private Long id;
        private String stepName;
        private Integer stepOrder;

        public StepDto(Long id, String stepName, Integer stepOrder) {
            this.id = id;
            this.stepName = stepName;
            this.stepOrder = stepOrder;
        }

        public Long getId() { return id; }
        public String getStepName() { return stepName; }
        public Integer getStepOrder() { return stepOrder; }
    }

    // Start production
    @PostMapping("/start-production")
    public ProductionRecordDto startProduction(@RequestParam Long productId) {
        return service.startProduction(productId);
    }

    // Start step
    @PostMapping("/start-step")
    public StepTimeResponseDto startStep(@RequestParam Long productionRecordId, @RequestParam String stepName) {
        return service.startStep(productionRecordId, stepName);
    }

    // Finish step
    @PostMapping("/finish-step")
    public StepTimeResponseDto finishStep(@RequestParam Long stepId) {
        return service.finishStep(stepId);
    }

    // Finish production
    @PostMapping("/finish-production")
    public ProductionRecordDto finishProduction(@RequestParam Long productionRecordId,
                                                @RequestParam String employeeName,
                                                @RequestParam Integer quantity,
                                                @RequestParam(required = false) String companyName) {
        return service.finishProduction(productionRecordId, employeeName, quantity, companyName);
    }

    // Get all products - also make it transactional
    @GetMapping("/products")
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        List<ProductManufacture> products = service.getAllProducts();
        return products.stream()
                .map(p -> new ProductDto(p.getId(), p.getProductName()))
                .toList();
    }

    public static class ProductDto {
        private Long id;
        private String productName;

        public ProductDto(Long id, String productName) {
            this.id = id;
            this.productName = productName;
        }
        public Long getId() { return id; }
        public String getProductName() { return productName; }
    }

    @GetMapping("/dashboard/production/daily")
    @Transactional(readOnly = true)
    public List<ProductionDashboardDto> getDailyProduction(@RequestParam LocalDate date) {
        return service.getDailyProduction(date);
    }

    @GetMapping("/active-records")
    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getActiveProductions() {
        return service.getActiveProductions();
    }

    // Add new endpoints for session-independent production
    @GetMapping("/active-productions")
    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getAllActiveProductions() {
        return service.getActiveProductions();
    }

//    @GetMapping("/my-active-productions")
//    @Transactional(readOnly = true)
//    public List<ProductionRecordDto> getMyActiveProductions() {
//        return service.getUserActiveProductions();
//    }

    @PostMapping("/resume-production")
    public ProductionRecordDto resumeProduction(@RequestParam Long productionRecordId) {
        return service.resumeProduction(productionRecordId);
    }

    @PostMapping("/cancel-production")
    public ProductionRecordDto cancelProduction(@RequestParam Long productionRecordId) {
        return service.cancelProduction(productionRecordId);
    }
    @PostMapping("/pause-step")
    public StepTimeResponseDto pauseStep(@RequestParam Long stepId) {
        return service.pauseStep(stepId);
    }

    @PostMapping("/pause-production")
    public ProductionRecordDto pauseProduction(@RequestParam Long productionRecordId) {
        return service.pauseProduction(productionRecordId);
    }

//    @GetMapping("/paused-productions")
//    @Transactional(readOnly = true)
//    public List<ProductionRecordDto> getPausedProductions() {
//        return service.getPausedProductions();
//    }

    @GetMapping("/production/{id}")
    @Transactional(readOnly = true)
    public ProductionRecordDto getProduction(@PathVariable Long id) {
        return service.getProductionWithElapsedTime(id);
    }

//    @GetMapping("/my-resumable-productions")
//    @Transactional(readOnly = true)
//    public List<ProductionRecordDto> getMyResumableProductions() {
//        return service.getUserResumableProductions();
//    }

    // Replace /my-active-productions with this:
    @GetMapping("/my-resumable-productions")
    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getMyResumableProductions() {
        return service.getResumableProductions();
    }

    // Keep existing endpoints for compatibility
    @GetMapping("/my-active-productions")
    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getMyActiveProductions() {
        return service.getUserActiveProductions();
    }

    @GetMapping("/paused-productions")
    @Transactional(readOnly = true)
    public List<ProductionRecordDto> getPausedProductions() {
        return service.getPausedProductions();
    }
}