package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalyticsDTO {
    private LocalDate date;
    private Long totalRevenue;
    private Integer totalOrders;
    private Integer totalItems;
    private Double averageOrderValue;
    private String timePeriod; // MORNING, AFTERNOON, EVENING, NIGHT
}
