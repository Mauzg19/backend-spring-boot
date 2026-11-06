package com.Restaurant.request;

import com.Restaurant.model.Address;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
 
    @NotBlank
    private String clientRequestId;

    @NotNull
    private Long restaurantId;
    
    @NotNull
    @Valid
    private Address deliveryAddress;

    @NotEmpty
    @Valid
    private List<CreateOrderItemRequest> items;

    private Long rewardId;

    private String rfcCliente;      // <-- Agrega este campo
    private String razonSocial;     // <-- Agrega este campo

}
