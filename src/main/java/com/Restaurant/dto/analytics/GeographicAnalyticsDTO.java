package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeographicAnalyticsDTO {
    private String city;
    private Long totalRevenue;
    private Integer totalOrders;
    private Integer totalCustomers;
    private Double averageOrderValue;
}
