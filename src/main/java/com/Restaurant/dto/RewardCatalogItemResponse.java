package com.Restaurant.dto;

public record RewardCatalogItemResponse(
		Long id,
		String code,
		String name,
		String description,
		int requiredPoints,
		long discountAmount,
		long minimumOrderAmount,
		boolean active) {
}
