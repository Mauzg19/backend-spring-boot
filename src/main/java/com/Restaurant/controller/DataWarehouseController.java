package com.Restaurant.controller;

import com.Restaurant.service.DataWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/datawarehouse")
public class DataWarehouseController {

    @Autowired
    private DataWarehouseService dataWarehouseService;

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> refreshDataWarehouse() {
        try {
            dataWarehouseService.initializeDataWarehouse();
            return ResponseEntity.ok("Data Warehouse refreshed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error refreshing Data Warehouse: " + e.getMessage());
        }
    }
}
