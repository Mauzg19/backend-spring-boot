package com.Restaurant.request;

import com.Restaurant.model.Address;

import lombok.Data;

@Data
public class CreateOrderRequest {
 
    private Long restaurantId;
    
    private Address deliveryAddress;

    private String rfcCliente;      // <-- Agrega este campo
    private String razonSocial;     // <-- Agrega este campo

    // Si no usas Lombok @Data, agrega los getters y setters manualmente:
    public String getRfcCliente() {
        return rfcCliente;
    }

    public void setRfcCliente(String rfcCliente) {
        this.rfcCliente = rfcCliente;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }
}
