package com.Restaurant.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.Exception.UserException;
import com.Restaurant.model.PaymentResponse;
import com.Restaurant.service.PaymentService;
import com.Restaurant.service.OrderService;
import com.Restaurant.service.UserService;
import com.Restaurant.model.User;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;

@RestController
@RequestMapping("/api")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private UserService userService;

	@Value("${stripe.checkout.return-url}")
	private String checkoutReturnUrl;

	@GetMapping("/order/{orderId}/payment-status")
	public ResponseEntity<PaymentResponse> getPaymentStatus(
			@PathVariable Long orderId,
			@RequestHeader("Authorization") String jwt) throws OrderException, StripeException, UserException {
		User user = userService.findUserProfileByJwt(jwt);
		return ResponseEntity.ok(orderService.getPaymentStatus(orderId, user));
	}

	@PostMapping("/payments/stripe/webhook")
	public ResponseEntity<Void> stripeWebhook(
			@RequestBody String payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String signature)
			throws StripeException {
		if (signature == null || signature.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		try {
			paymentService.handleWebhook(payload, signature);
		} catch (SignatureVerificationException exception) {
			return ResponseEntity.badRequest().build();
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
		} catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping("/payments/stripe/return")
	public ResponseEntity<Void> checkoutReturn(
			@RequestParam("session_id") String sessionId,
			@RequestParam(defaultValue = "false") boolean cancelled) throws StripeException {
		PaymentResponse result = paymentService.handleCheckoutReturn(sessionId, cancelled);
		URI redirect = UriComponentsBuilder.fromUriString(checkoutReturnUrl)
				.queryParam("order_id", result.getOrderId())
				.queryParam("payment_status", result.getPaymentStatus().toLowerCase())
				.build()
				.encode()
				.toUri();
		return ResponseEntity.status(HttpStatus.SEE_OTHER).location(redirect).build();
	}

}
