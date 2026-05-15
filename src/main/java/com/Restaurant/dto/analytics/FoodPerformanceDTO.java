package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodPerformanceDTO {
    private Long foodId;
    private String foodName;
    private Integer quantitySold;
    private Long totalRevenue;
    private Long price;
    private String categoryName;
    private Boolean vegetarian;
    private Boolean seasonal;
    private Double revenuePercentage;
}
