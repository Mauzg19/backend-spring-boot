package com.Restaurant.service;

import com.stripe.exception.StripeException;
import com.Restaurant.model.Order;
import com.Restaurant.model.PaymentResponse;

public interface PaymentService {
	
	public PaymentResponse generatePaymentLink(Order order) throws StripeException;

}
