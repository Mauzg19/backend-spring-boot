package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantPerformanceDTO {
    private Long restaurantId;
    private String restaurantName;
    private Long totalRevenue;
    private Integer totalOrders;
    private Integer averageRating;
    private Integer totalItems;
    private Double revenuePercentage;
    private String cuisineType;
}
