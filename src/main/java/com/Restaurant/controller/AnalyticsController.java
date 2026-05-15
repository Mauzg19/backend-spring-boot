package com.Restaurant.controller;

import com.Restaurant.dto.analytics.*;
import com.Restaurant.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/dashboard/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(analyticsService.getDashboardSummary());
    }

    @GetMapping("/sales/daily")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalesAnalyticsDTO>> getSalesByDay() {
        return ResponseEntity.ok(analyticsService.getSalesAnalyticsByDay());
    }

    @GetMapping("/sales/time-period")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalesAnalyticsDTO>> getSalesByTimePeriod() {
        return ResponseEntity.ok(analyticsService.getSalesAnalyticsByTimePeriod());
    }

    @GetMapping("/restaurants/performance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RestaurantPerformanceDTO>> getRestaurantPerformance() {
        return ResponseEntity.ok(analyticsService.getRestaurantPerformance());
    }

    @GetMapping("/food/top")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FoodPerformanceDTO>> getTopFoodItems(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopFoodItems(limit));
    }

    @GetMapping("/categories/performance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoryPerformanceDTO>> getCategoryPerformance() {
        return ResponseEntity.ok(analyticsService.getCategoryPerformance());
    }

    @GetMapping("/geographic/analysis")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GeographicAnalyticsDTO>> getGeographicAnalytics() {
        return ResponseEntity.ok(analyticsService.getGeographicAnalytics());
    }

    @GetMapping("/customers/top")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CustomerAnalyticsDTO>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopCustomers(limit));
    }
}
