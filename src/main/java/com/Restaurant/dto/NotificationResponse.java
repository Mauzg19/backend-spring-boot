package com.Restaurant.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        Instant sentAt,
        boolean read,
        Long orderId) {
}
