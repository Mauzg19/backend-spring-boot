package com.Restaurant.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.model.Order;
import com.Restaurant.model.Payment;
import com.Restaurant.model.PaymentResponse;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.Coupon;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

@Service
@Transactional
public class PaymentServiceImplementation implements PaymentService {

	@Value("${stripe.api.key:}")
	private String stripeSecretKey;

	@Value("${stripe.webhook.secret:}")
	private String stripeWebhookSecret;

	@Value("${stripe.checkout.return-url}")
	private String checkoutReturnUrl;

	@Value("${stripe.checkout.backend-url:http://localhost:5454}")
	private String checkoutBackendUrl;

	private final PaymentRepository paymentRepository;
	private final OrderRepository orderRepository;
	private final RewardsService rewardsService;
	private final NotificationService notificationService;

	public PaymentServiceImplementation(
			PaymentRepository paymentRepository,
			OrderRepository orderRepository,
			RewardsService rewardsService,
			NotificationService notificationService) {
		this.paymentRepository = paymentRepository;
		this.orderRepository = orderRepository;
		this.rewardsService = rewardsService;
		this.notificationService = notificationService;
	}

	@Override
	public PaymentResponse generatePaymentLink(Order order) throws StripeException {
		Payment payment = order.getPayment();
		if (payment == null || payment.getId() == null) {
			throw new IllegalStateException("El pedido no tiene un registro de pago persistido.");
		}
		if (!"PENDING".equals(payment.getPaymentStatus())) {
			return toResponse(order, payment);
		}
		if (StringUtils.hasText(payment.getStripeCheckoutSessionId())
				&& StringUtils.hasText(payment.getCheckoutUrl())) {
			return toResponse(order, payment);
		}

		configureStripe();
		long expiresAt = Instant.now().plus(Duration.ofHours(23)).getEpochSecond();
		SessionCreateParams.Builder params = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
				.setClientReferenceId(order.getId().toString())
				.putMetadata("order_id", order.getId().toString())
				.setSuccessUrl(checkoutReturnEndpoint()
						+ "?session_id={CHECKOUT_SESSION_ID}&cancelled=false")
				.setCancelUrl(checkoutReturnEndpoint()
						+ "?session_id={CHECKOUT_SESSION_ID}&cancelled=true")
				.putExtraParam("expires_at", expiresAt);

		for (var item : order.getItems()) {
			long amountInMinorUnits = Math.multiplyExact(item.getFood().getPrice(), 100L);
			SessionCreateParams.LineItem.PriceData.ProductData.Builder product =
					SessionCreateParams.LineItem.PriceData.ProductData.builder()
							.setName(item.getFood().getName());
			if (StringUtils.hasText(item.getNotes())) {
				product.setDescription(item.getNotes());
			}
			params.addLineItem(SessionCreateParams.LineItem.builder()
					.setQuantity((long) item.getQuantity())
					.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
							.setCurrency("cop")
							.setUnitAmount(amountInMinorUnits)
							.setProductData(product.build())
							.build())
					.build());
		}
		params.setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
				.putMetadata("order_id", order.getId().toString())
				.build());

		RequestOptions requestOptions = RequestOptions.builder()
				.setIdempotencyKey("restaurant-order-" + order.getId())
				.build();
		if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
			Coupon coupon = Coupon.create(CouponCreateParams.builder()
					.setAmountOff(Math.multiplyExact(order.getDiscountAmount(), 100L))
					.setCurrency("cop")
					.setDuration(CouponCreateParams.Duration.ONCE)
					.setMaxRedemptions(1L)
					.build(), RequestOptions.builder()
							.setIdempotencyKey("restaurant-order-reward-" + order.getId())
							.build());
			params.addDiscount(SessionCreateParams.Discount.builder()
					.setCoupon(coupon.getId())
					.build());
		}
		Session session = Session.create(params.build(), requestOptions);
		if (!StringUtils.hasText(session.getId()) || !StringUtils.hasText(session.getUrl())) {
			throw new IllegalStateException("Stripe no devolvió una sesión de Checkout completa.");
		}

		payment.setStripeCheckoutSessionId(session.getId());
		payment.setCheckoutUrl(session.getUrl());
		payment.setCheckoutExpiresAt(expiresAt);
		paymentRepository.save(payment);
		return toResponse(order, payment);
	}

	@Override
	public PaymentResponse refreshPaymentStatus(Order order) throws StripeException {
		Payment payment = requirePayment(order);
		payment = paymentRepository.findByIdForUpdate(payment.getId()).orElse(payment);
		if ("PAID".equals(payment.getPaymentStatus())
				|| "CANCELLED".equals(payment.getPaymentStatus())
				|| "FAILED".equals(payment.getPaymentStatus())) {
			return toResponse(order, payment);
		}
		if (!StringUtils.hasText(payment.getStripeCheckoutSessionId())) {
			throw new IllegalStateException("El pago todavía no tiene una sesión de Stripe.");
		}
		configureStripe();
		Session session = Session.retrieve(payment.getStripeCheckoutSessionId());
		validateSession(order, payment, session);
		synchronizeSession(order, payment, session,
				isSessionExpired(payment) ? "CANCELLED" : null, false);
		return toResponse(order, payment);
	}

	@Override
	public void handleWebhook(String payload, String signature) throws StripeException {
		if (!StringUtils.hasText(stripeWebhookSecret)) {
			throw new IllegalStateException("STRIPE_WEBHOOK_SECRET no está configurado.");
		}
		com.stripe.model.Event event;
		try {
			event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
		} catch (SignatureVerificationException exception) {
			throw exception;
		}

		String type = event.getType();
		if (!type.startsWith("checkout.session.")) {
			return;
		}
		Optional<StripeObject> eventObject = event.getDataObjectDeserializer().getObject();
		if (eventObject.isEmpty() || !(eventObject.get() instanceof Session session)) {
			throw new IllegalArgumentException("El evento Stripe no contiene una sesión de Checkout válida.");
		}

		Payment payment = paymentRepository.findByStripeCheckoutSessionId(session.getId())
				.orElseThrow(() -> new IllegalArgumentException("La sesión de Stripe no corresponde a un pago."));
		Order order = orderRepository.findById(payment.getOrderId())
				.orElseThrow(() -> new IllegalArgumentException("El pago no tiene un pedido asociado."));
		validateSession(order, payment, session);

		switch (type) {
			case "checkout.session.completed":
				synchronizeSession(order, payment, session, null, true);
				break;
			case "checkout.session.async_payment_succeeded":
				synchronizeSession(order, payment, session, "PAID", true);
				break;
			case "checkout.session.async_payment_failed":
				synchronizeSession(order, payment, session, "FAILED", true);
				break;
			case "checkout.session.expired":
				synchronizeSession(order, payment, session, "CANCELLED", true);
				break;
			default:
				break;
		}
	}

	@Override
	public PaymentResponse handleCheckoutReturn(String sessionId, boolean cancelled) throws StripeException {
		if (!StringUtils.hasText(sessionId)) {
			throw new IllegalArgumentException("Stripe no proporcionó el identificador de sesión.");
		}
		Payment payment = paymentRepository.findByStripeCheckoutSessionId(sessionId)
				.orElseThrow(() -> new IllegalArgumentException("La sesión de Stripe no corresponde a un pago."));
		Order order = orderRepository.findById(payment.getOrderId())
				.orElseThrow(() -> new IllegalArgumentException("El pago no tiene un pedido asociado."));
		if ("PAID".equals(payment.getPaymentStatus())
				|| "CANCELLED".equals(payment.getPaymentStatus())
				|| "FAILED".equals(payment.getPaymentStatus())) {
			return toResponse(order, payment);
		}
		configureStripe();
		Session session = Session.retrieve(sessionId);
		validateSession(order, payment, session);

		if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
			synchronizeSession(order, payment, session, "PAID", false);
		} else if (cancelled) {
			Session expiredSession = ApiResource.request(
					ApiResource.RequestMethod.POST,
					"/v1/checkout/sessions/" + sessionId + "/expire",
					Map.of(),
					Session.class,
					RequestOptions.builder().setApiKey(stripeSecretKey).build());
			validateSession(order, payment, expiredSession);
			synchronizeSession(order, payment, expiredSession, "CANCELLED", false);
		} else {
			synchronizeSession(order, payment, session,
					isSessionExpired(payment) ? "CANCELLED" : null, false);
		}
		return toResponse(order, payment);
	}

	private void configureStripe() {
		if (!StringUtils.hasText(stripeSecretKey)) {
			throw new IllegalStateException("STRIPE_SECRET_KEY no está configurado.");
		}
		Stripe.apiKey = stripeSecretKey;
	}

	private String checkoutReturnEndpoint() {
		return checkoutBackendUrl.replaceAll("/+$", "") + "/api/payments/stripe/return";
	}

	private boolean isSessionExpired(Payment payment) {
		return payment.getCheckoutExpiresAt() != null
				&& Instant.now().getEpochSecond() >= payment.getCheckoutExpiresAt();
	}

	private Payment requirePayment(Order order) {
		if (order.getPayment() == null) {
			throw new IllegalStateException("El pedido no tiene un pago asociado.");
		}
		return order.getPayment();
	}

	private void validateSession(Order order, Payment payment, Session session) {
		Map<String, String> metadata = session.getMetadata();
		String expectedOrderId = order.getId().toString();
		long expectedAmount = Math.multiplyExact(order.getTotalAmount(), 100L);
		if (!payment.getStripeCheckoutSessionId().equals(session.getId())
				|| !expectedOrderId.equals(session.getClientReferenceId())
				|| metadata == null
				|| !expectedOrderId.equals(metadata.get("order_id"))
				|| !"cop".equalsIgnoreCase(session.getCurrency())
				|| session.getAmountTotal() == null
				|| session.getAmountTotal() != expectedAmount) {
			throw new IllegalArgumentException("La sesión de Stripe no coincide con el pedido y su importe.");
		}
	}

	private void synchronizeSession(
			Order order,
			Payment payment,
			Session session,
			String explicitStatus,
			boolean signedWebhook) {
		if ("PAID".equals(payment.getPaymentStatus())) {
			if (signedWebhook) {
				rewardsService.creditPaidOrder(order);
			}
			return;
		}
		if ("CANCELLED".equals(payment.getPaymentStatus())
				|| "FAILED".equals(payment.getPaymentStatus())) {
			return;
		}

		String nextStatus = explicitStatus;
		if (nextStatus == null && "paid".equalsIgnoreCase(session.getPaymentStatus())) {
			nextStatus = "PAID";
		}
		if (nextStatus == null) {
			return;
		}

		payment.setPaymentStatus(nextStatus);
		if ("PAID".equals(nextStatus)) {
			payment.setStripePaymentIntentId(session.getPaymentIntent());
			payment.setCreatedAt(new Date());
			order.setOrderStatus("PENDING");
			if (signedWebhook) {
				rewardsService.creditPaidOrder(order);
			}
		} else if ("FAILED".equals(nextStatus)) {
			order.setOrderStatus("PAYMENT_FAILED");
			rewardsService.releaseReservedPoints(order);
		} else if ("CANCELLED".equals(nextStatus)) {
			order.setOrderStatus("CANCELLED");
			rewardsService.releaseReservedPoints(order);
		}
		paymentRepository.save(payment);
		orderRepository.save(order);
		if ("PAID".equals(nextStatus)) {
			notificationService.sendNewOrderNotification(order);
		}
	}

	private PaymentResponse toResponse(Order order, Payment payment) {
		PaymentResponse response = new PaymentResponse();
		response.setPaymentUrl(payment.getCheckoutUrl());
		response.setOrderId(order.getId());
		response.setCheckoutSessionId(payment.getStripeCheckoutSessionId());
		response.setPaymentStatus(payment.getPaymentStatus());
		response.setOrderStatus(order.getOrderStatus());
		response.setSubtotalAmount(order.getSubtotalAmount() == null
				? order.getTotalAmount()
				: order.getSubtotalAmount());
		response.setDiscountAmount(order.getDiscountAmount() == null ? 0L : order.getDiscountAmount());
		response.setTotalAmount(order.getTotalAmount());
		response.setRewardPointsUsed(order.getRewardPointsReserved());
		return response;
	}
}
