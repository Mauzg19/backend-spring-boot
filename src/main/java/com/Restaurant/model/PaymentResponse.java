package com.Restaurant.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class PaymentResponse {

	@JsonProperty("payment_url")
	private String paymentUrl;

	@JsonProperty("order_id")
	private Long orderId;

	@JsonProperty("checkout_session_id")
	private String checkoutSessionId;

	@JsonProperty("payment_status")
	private String paymentStatus;

	@JsonProperty("order_status")
	private String orderStatus;

	@JsonProperty("subtotal_amount")
	private Long subtotalAmount;

	@JsonProperty("discount_amount")
	private Long discountAmount;

	@JsonProperty("total_amount")
	private Long totalAmount;

	@JsonProperty("reward_points_used")
	private Integer rewardPointsUsed;
}
