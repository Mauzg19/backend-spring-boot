package com.Restaurant.service;

import com.stripe.exception.StripeException;
import com.Restaurant.model.Order;
import com.Restaurant.model.PaymentResponse;
public interface PaymentService {
	
	public PaymentResponse generatePaymentLink(Order order) throws StripeException;

	PaymentResponse refreshPaymentStatus(Order order) throws StripeException;

	void handleWebhook(String payload, String signature) throws StripeException;

	PaymentResponse handleCheckoutReturn(String sessionId, boolean cancelled) throws StripeException;
}
