package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformanceDTO {
    private Long categoryId;
    private String categoryName;
    private Integer quantitySold;
    private Long totalRevenue;
    private Integer numberOfDishes;
    private Double revenuePercentage;
    private String topDish;
}
