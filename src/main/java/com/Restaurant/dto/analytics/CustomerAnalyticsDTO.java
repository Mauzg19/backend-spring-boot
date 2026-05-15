package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsDTO {
    private Long customerId;
    private String customerName;
    private Integer totalOrders;
    private Long totalSpent;
    private Double averageOrderValue;
    private String paymentMethod;
    private String lastOrderDate;
}
