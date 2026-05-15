package com.Restaurant.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiMetricDTO {
    private String label;
    private Object value;
    private String unit;
    private String trend; // UP, DOWN, STABLE
}
