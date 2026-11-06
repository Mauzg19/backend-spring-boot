package com.Restaurant.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateOrderItemRequest {

    @NotNull
    private Long menuItemId;

    @Positive
    private int quantity;

    private String notes;
}
