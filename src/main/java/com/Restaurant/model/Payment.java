package com.Restaurant.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String paymentMethod;
    private String paymentStatus;
    private double totalAmount;

    @Column(unique = true)
    private String stripeCheckoutSessionId;

    @Column(length = 2048)
    @JsonIgnore
    private String checkoutUrl;

    private String stripePaymentIntentId;

    private Long checkoutExpiresAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    
}
