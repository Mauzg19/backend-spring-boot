package com.Restaurant.controller;

import com.Restaurant.service.DemandModelService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/demand")
@PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
public class DemandController {
    private final DemandModelService service;
    public DemandController(DemandModelService service) { this.service = service; }

    @GetMapping("/report")
    public JsonNode report() { return service.report(); }

    public record Prediction(@NotBlank String product, @NotNull LocalDate date,
            @NotNull @DecimalMin("0") @DecimalMax("10000") Double lag1,
            @NotNull @DecimalMin("0") @DecimalMax("10000") Double lag7,
            @NotNull @DecimalMin("0") @DecimalMax("10000") Double mean7) {}

    @PostMapping("/predict")
    public Map<String, Object> predict(@Valid @RequestBody Prediction input) {
        return service.predict(input.product(), input.date(), input.lag1(), input.lag7(), input.mean7());
    }
}
