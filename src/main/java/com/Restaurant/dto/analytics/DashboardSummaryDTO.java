package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private Long totalRevenue;
    private Integer totalOrders;
    private Integer totalCustomers;
    private Integer totalRestaurants;
    private Double averageOrderValue;
    private Integer topPerformerRestaurants;
    private String bestSellingCategory;
    private Integer uniqueCities;
    private List<KpiMetricDTO> kpis;
}
