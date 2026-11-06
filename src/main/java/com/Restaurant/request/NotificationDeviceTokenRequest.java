package com.Restaurant.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationDeviceTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotBlank @Size(max = 24) String platform) {
}
