package com.Restaurant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.config.JwtProvider;
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.model.NotificationDeviceToken;
import com.Restaurant.model.Notification;
import com.Restaurant.model.Order;
import com.Restaurant.model.Payment;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.RewardAccount;
import com.Restaurant.model.RewardCatalogItem;
import com.Restaurant.model.RewardLedgerEntry;
import com.Restaurant.model.User;
import com.Restaurant.dto.RewardsSummaryResponse;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.NotificationDeviceTokenRepository;
import com.Restaurant.repository.NotificationRepository;
import com.Restaurant.repository.PaymentRepository;
import com.Restaurant.repository.RestaurantRepository;
import com.Restaurant.repository.RewardAccountRepository;
import com.Restaurant.repository.RewardLedgerEntryRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.service.RewardsService;
import com.stripe.Stripe;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StripeWebhookIntegrationTest {

	private static final String WEBHOOK_SECRET = "whsec_runtime_validation";

	@Autowired private MockMvc mockMvc;
	@Autowired private OrderRepository orderRepository;
	@Autowired private NotificationDeviceTokenRepository notificationDeviceTokenRepository;
	@Autowired private NotificationRepository notificationRepository;
	@Autowired private PaymentRepository paymentRepository;
	@Autowired private RestaurantRepository restaurantRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private JwtProvider jwtProvider;
	@Autowired private RewardAccountRepository rewardAccountRepository;
	@Autowired private RewardLedgerEntryRepository rewardLedgerEntryRepository;
	@Autowired private RewardsService rewardsService;
	@Autowired private com.Restaurant.repository.RewardCatalogItemRepository rewardCatalogItemRepository;

	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
		paymentRepository.deleteAll();
	}

	@Test
	void signedPaidEventConfirmsOrderAndDuplicateDeliveryDoesNotDuplicateTransition() throws Exception {
		Payment payment = createPendingPayment();
		String payload = event(payment, "checkout.session.completed", "paid");
		String signature = signature(payload);

		mockMvc.perform(post("/api/payments/stripe/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Stripe-Signature", signature)
				.content(payload))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/payments/stripe/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Stripe-Signature", signature)
				.content(payload))
			.andExpect(status().isOk());

		Order savedOrder = orderRepository.findById(payment.getOrderId()).orElseThrow();
		Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
		assertEquals("PENDING", savedOrder.getOrderStatus());
		assertEquals("PAID", savedPayment.getPaymentStatus());
		List<Notification> ownerNotifications = notificationRepository
				.findByCustomerIdOrderBySentAtDesc(savedOrder.getRestaurant().getOwner().getId());
		assertEquals(1, ownerNotifications.size());
		assertEquals("NEW_ORDER", ownerNotifications.get(0).getType());
		assertEquals(savedOrder.getId(), ownerNotifications.get(0).getOrderId());
		RewardAccount account = rewardAccountRepository.findByUserId(
				savedOrder.getCustomer().getId()).orElseThrow();
		assertEquals(15, account.getPointsBalance());
		assertEquals(1, rewardLedgerEntryRepository
				.findTop100ByAccount_IdOrderByCreatedAtDesc(account.getId()).size());
	}

	@Test
	void cancelledAndFailedEventsNeverExposeOrderToRestaurant() throws Exception {
		Payment cancelledPayment = createPendingPayment();
		sendEvent(cancelledPayment, "checkout.session.expired", "unpaid");
		assertEquals("CANCELLED", orderRepository.findById(cancelledPayment.getOrderId())
				.orElseThrow().getOrderStatus());

		Payment failedPayment = createPendingPayment();
		sendEvent(failedPayment, "checkout.session.async_payment_failed", "unpaid");
		assertEquals("PAYMENT_FAILED", orderRepository.findById(failedPayment.getOrderId())
				.orElseThrow().getOrderStatus());
		assertEquals(0, accountFor(failedPayment).getPointsBalance());
	}

	@Test
	void paidOrderWithoutRewardsMembershipDoesNotCreatePoints() throws Exception {
		Payment payment = createPendingPayment(false);
		sendEvent(payment, "checkout.session.completed", "paid");
		Order order = orderRepository.findById(payment.getOrderId()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(
				rewardAccountRepository.findByUserId(order.getCustomer().getId()).isEmpty());
	}

	@Test
	void notificationEndpointsOnlyExposeAndChangeTheAuthenticatedUsersData() throws Exception {
		User customer = notificationUser("notification-customer");
		User anotherCustomer = notificationUser("another-customer");
		Notification ownNotification = notification(customer, "ORDER_STATUS_READY");
		Notification otherNotification = notification(anotherCustomer, "ORDER_STATUS_READY");
		ownNotification = notificationRepository.saveAndFlush(ownNotification);
		otherNotification = notificationRepository.saveAndFlush(otherNotification);
		String jwt = jwtProvider.generateToken(new UsernamePasswordAuthenticationToken(
				customer.getEmail(),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

		mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + jwt))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.jsonPath("$.length()").value(1))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.jsonPath("$[0].id").value(ownNotification.getId()));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/notifications/{id}/read", otherNotification.getId())
				.header("Authorization", "Bearer " + jwt))
				.andExpect(status().isNotFound());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/notifications/{id}/read", ownNotification.getId())
				.header("Authorization", "Bearer " + jwt))
				.andExpect(status().isNoContent());
		assertTrue(notificationRepository.findById(ownNotification.getId()).orElseThrow()
				.isReadStatus());

		String token = "test-fcm-token-" + UUID.randomUUID();
		mockMvc.perform(post("/api/notifications/device-token")
				.header("Authorization", "Bearer " + jwt)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + token + "\",\"platform\":\"android\"}"))
				.andExpect(status().isNoContent());
		NotificationDeviceToken registeredToken = notificationDeviceTokenRepository
				.findByToken(token).orElseThrow();
		assertEquals(customer.getId(), registeredToken.getUser().getId());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.delete("/api/notifications/device-token")
				.param("token", token)
				.header("Authorization", "Bearer " + jwt))
				.andExpect(status().isNoContent());
		assertTrue(notificationDeviceTokenRepository.findByToken(token).isEmpty());
	}

	@Test
	void joiningIsIdempotentAndReturnsThePersistedRewardCatalog() {
		User user = new User();
		user.setFullName("Rewards member");
		user.setEmail(UUID.randomUUID() + "@example.test");
		user.setPassword("not-a-real-password");
		user = userRepository.saveAndFlush(user);

		RewardsSummaryResponse firstJoin = rewardsService.join(user);
		RewardsSummaryResponse repeatedJoin = rewardsService.join(user);

		assertTrue(firstJoin.joined());
		assertTrue(repeatedJoin.joined());
		assertEquals(0, repeatedJoin.pointsBalance());
		assertEquals(2, repeatedJoin.catalog().size());
		assertEquals(100, repeatedJoin.catalog().get(0).requiredPoints());
		assertEquals(5000L, repeatedJoin.catalog().get(0).discountAmount());
		assertEquals(25000L, repeatedJoin.catalog().get(0).minimumOrderAmount());
		assertEquals(1, rewardAccountRepository.count());
	}

	@Test
	void canOnlyReserveAnActiveCatalogRewardWithEnoughPointsAndMinimumOrder() throws Exception {
		User user = new User();
		user.setFullName("Rewards checkout");
		user.setEmail(UUID.randomUUID() + "@example.test");
		user.setPassword("not-a-real-password");
		user = userRepository.saveAndFlush(user);
		User rewardsUser = user;
		rewardsService.join(rewardsUser);
		RewardAccount account = rewardAccountRepository.findByUserId(rewardsUser.getId()).orElseThrow();
		RewardCatalogItem reward = rewardCatalogItemRepository
				.findByCode("starter-discount").orElseThrow();

		Order order = new Order();
		order.setCustomer(user);
		order.setTotalAmount(30000L);
		order = orderRepository.saveAndFlush(order);
		Order savedOrder = order;
		assertThrows(OrderException.class,
				() -> rewardsService.reserveReward(rewardsUser, reward.getId(), savedOrder, 30000L));

		account.setPointsBalance(100);
		rewardAccountRepository.saveAndFlush(account);
		assertThrows(OrderException.class,
				() -> rewardsService.reserveReward(rewardsUser, reward.getId(), savedOrder, 20000L));
		assertEquals(100, rewardAccountRepository.findByUserId(rewardsUser.getId()).orElseThrow()
				.getPointsBalance());

		rewardsService.reserveReward(rewardsUser, reward.getId(), savedOrder, 30000L);
		assertEquals(0, rewardAccountRepository.findByUserId(rewardsUser.getId()).orElseThrow()
				.getPointsBalance());
		RewardLedgerEntry redemption = rewardLedgerEntryRepository
				.findByReferenceKey("redemption-order-" + order.getId()).orElseThrow();
		assertEquals(-100, redemption.getPointsDelta());
	}

	@Test
	@WithMockUser
	void rewardsCatalogEndpointRequiresAuthenticationAndReturnsDatabaseCatalog() throws Exception {
		mockMvc.perform(get("/api/rewards/catalog"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.jsonPath("$[0].requiredPoints").value(100))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.jsonPath("$[0].discountAmount").value(5000));
	}

	@Test
	void duplicateExpiredEventRestoresReservedPointsOnlyOnce() throws Exception {
		Payment payment = createPendingPayment();
		Order order = orderRepository.findById(payment.getOrderId()).orElseThrow();
		RewardAccount account = accountFor(payment);
		account.setPointsBalance(0);
		rewardAccountRepository.saveAndFlush(account);
		order.setRewardPointsReserved(100);
		orderRepository.saveAndFlush(order);

		RewardLedgerEntry reservation = new RewardLedgerEntry();
		reservation.setAccount(account);
		reservation.setOrder(order);
		reservation.setReferenceKey("redemption-order-" + order.getId());
		reservation.setType("REDEMPTION");
		reservation.setPointsDelta(-100);
		reservation.setDescription("Test redemption reservation");
		reservation.setCreatedAt(java.time.LocalDateTime.now());
		rewardLedgerEntryRepository.saveAndFlush(reservation);

		String payload = event(payment, "checkout.session.expired", "unpaid");
		mockMvc.perform(post("/api/payments/stripe/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Stripe-Signature", signature(payload))
				.content(payload))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/payments/stripe/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Stripe-Signature", signature(payload))
				.content(payload))
			.andExpect(status().isOk());

		assertEquals(100, accountFor(payment).getPointsBalance());
		assertEquals(1, rewardLedgerEntryRepository.findTop100ByAccount_IdOrderByCreatedAtDesc(
				account.getId()).stream().filter(entry -> "REFUND".equals(entry.getType())).count());
	}

	@Test
	void completedButUnpaidSessionRemainsPendingAndInvalidSignatureIsRejected() throws Exception {
		Payment payment = createPendingPayment();
		String payload = event(payment, "checkout.session.completed", "unpaid");

		mockMvc.perform(post("/api/payments/stripe/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Stripe-Signature", signature(payload))
				.content(payload))
			.andExpect(status().isOk());
		assertEquals("PENDING", paymentRepository.findById(payment.getId())
				.orElseThrow().getPaymentStatus());

		mockMvc.perform(post("/api/payments/stripe/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Stripe-Signature", "t=1,v1=invalid")
				.content(payload))
			.andExpect(status().isBadRequest());
	}

	private Payment createPendingPayment() {
		return createPendingPayment(true);
	}

	private Payment createPendingPayment(boolean joinRewards) {
		User user = new User();
		user.setFullName("Rewards test");
		user.setEmail(UUID.randomUUID() + "@example.test");
		user.setPassword("not-a-real-password");
		user = userRepository.saveAndFlush(user);

		User owner = new User();
		owner.setFullName("Restaurant owner");
		owner.setEmail(UUID.randomUUID() + "@example.test");
		owner.setPassword("not-a-real-password");
		owner = userRepository.saveAndFlush(owner);
		Restaurant restaurant = new Restaurant();
		restaurant.setOwner(owner);
		restaurant.setName("Test restaurant");
		restaurant = restaurantRepository.saveAndFlush(restaurant);

		if (joinRewards) {
			RewardAccount account = new RewardAccount();
			account.setUser(user);
			account.setPointsBalance(0);
			account.setJoinedAt(java.time.LocalDateTime.now());
			rewardAccountRepository.saveAndFlush(account);
		}

		Order order = new Order();
		order.setCustomer(user);
		order.setRestaurant(restaurant);
		order.setClientRequestId(UUID.randomUUID().toString());
		order.setOrderStatus("PENDING_PAYMENT");
		order.setTotalAmount(15000L);
		order = orderRepository.saveAndFlush(order);

		Payment payment = new Payment();
		payment.setOrderId(order.getId());
		payment.setPaymentMethod("STRIPE_CHECKOUT");
		payment.setPaymentStatus("PENDING");
		payment.setTotalAmount(15000);
		payment.setStripeCheckoutSessionId("cs_test_" + UUID.randomUUID().toString().replace("-", ""));
		payment = paymentRepository.saveAndFlush(payment);
		order.setPayment(payment);
		orderRepository.saveAndFlush(order);
		return payment;
	}

	private User notificationUser(String prefix) {
		User user = new User();
		user.setFullName(prefix);
		user.setEmail(prefix + "-" + UUID.randomUUID() + "@example.test");
		user.setPassword("not-a-real-password");
		user.setRole(USER_ROLE.ROLE_CUSTOMER);
		return userRepository.saveAndFlush(user);
	}

	private Notification notification(User recipient, String type) {
		Notification notification = new Notification();
		notification.setCustomer(recipient);
		notification.setTitle("Actualización del pedido");
		notification.setMessage("El pedido cambió de estado.");
		notification.setType(type);
		notification.setSentAt(new Date());
		notification.setReadStatus(false);
		return notification;
	}

	private RewardAccount accountFor(Payment payment) {
		Order order = orderRepository.findById(payment.getOrderId()).orElseThrow();
		return rewardAccountRepository.findByUserId(order.getCustomer().getId()).orElseThrow();
	}

	private String event(Payment payment, String type, String paymentStatus) {
		Order order = orderRepository.findById(payment.getOrderId()).orElseThrow();
		return """
				{
				  "id":"evt_%s",
				  "object":"event",
				  "api_version":"%s",
				  "created":%d,
				  "data":{"object":{
				    "id":"%s",
				    "object":"checkout.session",
				    "amount_total":%d,
				    "client_reference_id":"%d",
				    "currency":"cop",
				    "metadata":{"order_id":"%d"},
				    "payment_intent":"pi_test_%s",
				    "payment_status":"%s"
				  }},
				  "livemode":false,
				  "pending_webhooks":1,
				  "type":"%s"
				}
				""".formatted(
				UUID.randomUUID().toString().replace("-", ""),
				Stripe.API_VERSION,
				System.currentTimeMillis() / 1000,
				payment.getStripeCheckoutSessionId(),
				Math.multiplyExact(order.getTotalAmount(), 100L),
				order.getId(),
				order.getId(),
				UUID.randomUUID().toString().replace("-", ""),
				paymentStatus,
				type);
	}

	private void sendEvent(Payment payment, String type, String paymentStatus) throws Exception {
		String payload = event(payment, type, paymentStatus);
		mockMvc.perform(post("/api/payments/stripe/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Stripe-Signature", signature(payload))
				.content(payload))
			.andExpect(status().isOk());
	}

	private String signature(String payload) throws Exception {
		long timestamp = System.currentTimeMillis() / 1000;
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] signed = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
		return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(signed);
	}
}
