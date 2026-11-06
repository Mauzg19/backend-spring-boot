package com.Restaurant.dto;

import java.time.LocalDateTime;

public record RewardLedgerEntryResponse(
		Long id,
		String type,
		int pointsDelta,
		String description,
		Long orderId,
		LocalDateTime createdAt) {
}
