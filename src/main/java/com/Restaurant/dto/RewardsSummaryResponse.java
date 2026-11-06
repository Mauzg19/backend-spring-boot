package com.Restaurant.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RewardsSummaryResponse(
		boolean joined,
		int pointsBalance,
		LocalDateTime joinedAt,
		List<RewardCatalogItemResponse> catalog) {
}
