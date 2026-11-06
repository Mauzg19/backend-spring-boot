package com.Restaurant.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.model.Address;
import com.Restaurant.model.Food;
import com.Restaurant.model.Order;
import com.Restaurant.model.OrderItem;
import com.Restaurant.model.Payment;
import com.Restaurant.model.PaymentResponse;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.RewardCatalogItem;
import com.Restaurant.model.User;
import com.Restaurant.repository.AddressRepository;
import com.Restaurant.repository.OrderItemRepository;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.PaymentRepository;
import com.Restaurant.repository.RestaurantRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.repository.foodRepository;
import com.Restaurant.request.CreateOrderItemRequest;
import com.Restaurant.request.CreateOrderRequest;

@ExtendWith(MockitoExtension.class)
class OrderServiceCheckoutTest {

	@Mock private AddressRepository addressRepository;
	@Mock private OrderItemRepository orderItemRepository;
	@Mock private OrderRepository orderRepository;
	@Mock private PaymentRepository paymentRepository;
	@Mock private RestaurantRepository restaurantRepository;
	@Mock private foodRepository foodRepository;
	@Mock private UserRepository userRepository;
	@Mock private PaymentService paymentService;
	@Mock private NotificationService notificationService;
	@Mock private RewardsService rewardsService;
	@InjectMocks private OrderServiceImplementation orderService;

	void setUpCreateStubs() {
		when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
			OrderItem item = invocation.getArgument(0);
			item.setId(9L);
			return item;
		});
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			order.setId(42L);
			return order;
		});
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(7L);
			return payment;
		});
	}

	@Test
	void createsPendingOrderFromServerPricedItemsAndReturnsCheckoutSession() throws Exception {
		setUpCreateStubs();
		Restaurant restaurant = new Restaurant();
		restaurant.setId(3L);
		Food food = new Food();
		food.setId(11L);
		food.setName("Arepa");
		food.setPrice(15000L);
		food.setAvailable(true);
		food.setRestaurant(restaurant);
		when(restaurantRepository.findById(3L)).thenReturn(Optional.of(restaurant));
		when(foodRepository.findById(11L)).thenReturn(Optional.of(food));
		when(orderRepository.findByClientRequestId("checkout-123")).thenReturn(Optional.empty());

		PaymentResponse response = new PaymentResponse();
		response.setOrderId(42L);
		response.setPaymentStatus("PENDING");
		when(paymentService.generatePaymentLink(any(Order.class))).thenReturn(response);

		User user = new User();
		user.setId(5L);
		user.setAddresses(new ArrayList<>());
		CreateOrderItemRequest requestedItem = new CreateOrderItemRequest();
		requestedItem.setMenuItemId(11L);
		requestedItem.setQuantity(2);
		requestedItem.setNotes("Sin salsa");
		CreateOrderRequest request = new CreateOrderRequest();
		request.setClientRequestId("checkout-123");
		request.setRestaurantId(3L);
		request.setDeliveryAddress(new Address());
		request.setItems(List.of(requestedItem));

		orderService.createOrder(request, user);

		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository, org.mockito.Mockito.times(2)).save(orderCaptor.capture());
		Order persistedOrder = orderCaptor.getAllValues().get(1);
		assertEquals("PENDING_PAYMENT", persistedOrder.getOrderStatus());
		assertEquals(30000L, persistedOrder.getTotalAmount());
		assertEquals("Sin salsa", persistedOrder.getItems().get(0).getNotes());
		assertEquals(2, persistedOrder.getItems().get(0).getQuantity());
		verify(paymentRepository).save(any(Payment.class));
		verify(paymentService).generatePaymentLink(any(Order.class));
		verify(orderItemRepository).save(any(OrderItem.class));
	}

	@Test
	void appliesServerCatalogDiscountAndReservesPointsBeforeCheckout() throws Exception {
		setUpCreateStubs();
		Restaurant restaurant = new Restaurant();
		restaurant.setId(3L);
		Food food = new Food();
		food.setId(11L);
		food.setName("Arepa");
		food.setPrice(30000L);
		food.setAvailable(true);
		food.setRestaurant(restaurant);
		when(restaurantRepository.findById(3L)).thenReturn(Optional.of(restaurant));
		when(foodRepository.findById(11L)).thenReturn(Optional.of(food));
		when(orderRepository.findByClientRequestId("checkout-reward")).thenReturn(Optional.empty());

		RewardCatalogItem reward = new RewardCatalogItem();
		reward.setId(1L);
		reward.setRequiredPoints(100);
		reward.setDiscountAmount(5000L);
		when(rewardsService.reserveReward(any(User.class), org.mockito.ArgumentMatchers.eq(1L),
				any(Order.class), org.mockito.ArgumentMatchers.eq(30000L))).thenReturn(reward);

		PaymentResponse response = new PaymentResponse();
		response.setOrderId(42L);
		when(paymentService.generatePaymentLink(any(Order.class))).thenReturn(response);
		CreateOrderRequest request = request("checkout-reward");
		request.setRewardId(1L);

		orderService.createOrder(request, customer());

		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository, org.mockito.Mockito.times(3)).save(orderCaptor.capture());
		Order checkoutOrder = orderCaptor.getAllValues().get(2);
		assertEquals(30000L, checkoutOrder.getSubtotalAmount());
		assertEquals(5000L, checkoutOrder.getDiscountAmount());
		assertEquals(25000L, checkoutOrder.getTotalAmount());
		assertEquals(100, checkoutOrder.getRewardPointsReserved());
		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(paymentCaptor.capture());
		assertEquals(25000.0, paymentCaptor.getValue().getTotalAmount());
		verify(rewardsService).reserveReward(any(User.class), org.mockito.ArgumentMatchers.eq(1L),
				any(Order.class), org.mockito.ArgumentMatchers.eq(30000L));
	}

	@Test
	void reusesAnExistingOrderForTheSameClientRequestId() throws Exception {
		Order existing = new Order();
		User customer = new User();
		customer.setId(5L);
		existing.setCustomer(customer);
		existing.setId(42L);
		Restaurant restaurant = new Restaurant();
		restaurant.setId(3L);
		existing.setRestaurant(restaurant);
		existing.setDeliveryAddress(new Address());
		Food food = new Food();
		food.setId(11L);
		OrderItem item = new OrderItem();
		item.setFood(food);
		item.setQuantity(1);
		existing.setItems(List.of(item));
		when(orderRepository.findByClientRequestId("checkout-123")).thenReturn(Optional.of(existing));
		PaymentResponse response = new PaymentResponse();
		when(paymentService.generatePaymentLink(existing)).thenReturn(response);

		assertSame(response, orderService.createOrder(
				request("checkout-123"), customer));

		verify(orderRepository, never()).save(any(Order.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	void rejectsDifferentCartContentsForAnExistingClientRequestId() throws Exception {
		Order existing = new Order();
		User customer = new User();
		customer.setId(5L);
		existing.setCustomer(customer);
		existing.setId(42L);
		Restaurant restaurant = new Restaurant();
		restaurant.setId(3L);
		existing.setRestaurant(restaurant);
		existing.setDeliveryAddress(new Address());
		Food food = new Food();
		food.setId(11L);
		OrderItem item = new OrderItem();
		item.setFood(food);
		item.setQuantity(2);
		existing.setItems(List.of(item));
		when(orderRepository.findByClientRequestId("checkout-123")).thenReturn(Optional.of(existing));

		assertThrows(OrderException.class, () -> orderService.createOrder(
				request("checkout-123"), customer));

		verify(paymentService, never()).generatePaymentLink(any(Order.class));
	}

	@Test
	void rejectsProductsFromAnotherRestaurantBeforeCreatingCheckout() throws Exception {
		Restaurant selectedRestaurant = new Restaurant();
		selectedRestaurant.setId(3L);
		Restaurant differentRestaurant = new Restaurant();
		differentRestaurant.setId(4L);
		Food food = new Food();
		food.setId(11L);
		food.setPrice(1000L);
		food.setAvailable(true);
		food.setRestaurant(differentRestaurant);
		when(orderRepository.findByClientRequestId("checkout-123")).thenReturn(Optional.empty());
		when(restaurantRepository.findById(3L)).thenReturn(Optional.of(selectedRestaurant));
		when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(foodRepository.findById(11L)).thenReturn(Optional.of(food));

		org.junit.jupiter.api.Assertions.assertThrows(
				OrderException.class,
				() -> orderService.createOrder(request("checkout-123"), customer()));

		verify(paymentService, never()).generatePaymentLink(any(Order.class));
	}

	@Test
	void onlyReturnsPaidOrdersToRestaurantAndBlocksKitchenTransitionsBeforePayment() throws Exception {
		Order pending = orderWithPaymentStatus(1L, "PENDING");
		Order paid = orderWithPaymentStatus(2L, "PAID");
		when(orderRepository.findOrdersByRestaurantId(3L)).thenReturn(List.of(pending, paid));
		when(orderRepository.findById(1L)).thenReturn(Optional.of(pending));

		assertEquals(List.of(paid), orderService.getOrdersOfRestaurant(3L, null));
		assertThrows(OrderException.class, () -> orderService.updateOrder(1L, "PREPARING"));
		verify(notificationService, never()).sendOrderStatusNotification(any(Order.class));
	}

	@Test
	void onlyTheOwningRestaurantCanChangeAnOrderAndNotifyItsCustomer() throws Exception {
		User owner = new User();
		owner.setId(12L);
		owner.setRole(USER_ROLE.ROLE_RESTAURANT_OWNER);
		User otherOwner = new User();
		otherOwner.setId(13L);
		otherOwner.setRole(USER_ROLE.ROLE_RESTAURANT_OWNER);
		Restaurant restaurant = new Restaurant();
		restaurant.setOwner(owner);
		Order order = orderWithPaymentStatus(43L, "PAID");
		order.setOrderStatus("PENDING");
		order.setRestaurant(restaurant);
		when(orderRepository.findById(43L)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertThrows(
				OrderException.class,
				() -> orderService.updateOrder(43L, "PREPARING", otherOwner));
		verify(orderRepository, never()).save(any(Order.class));
		verify(notificationService, never()).sendOrderStatusNotification(any(Order.class));

		Order updated = orderService.updateOrder(43L, "PREPARING", owner);

		assertEquals("PREPARING", updated.getOrderStatus());
		verify(notificationService).sendOrderStatusNotification(updated);
	}

	private Order orderWithPaymentStatus(Long id, String paymentStatus) {
		Payment payment = new Payment();
		payment.setPaymentStatus(paymentStatus);
		Order order = new Order();
		order.setId(id);
		order.setPayment(payment);
		return order;
	}

	private CreateOrderRequest request(String requestId) {
		CreateOrderItemRequest item = new CreateOrderItemRequest();
		item.setMenuItemId(11L);
		item.setQuantity(1);
		CreateOrderRequest request = new CreateOrderRequest();
		request.setClientRequestId(requestId);
		request.setRestaurantId(3L);
		request.setDeliveryAddress(new Address());
		request.setItems(List.of(item));
		return request;
	}

	private User customer() {
		User user = new User();
		user.setId(5L);
		user.setAddresses(new ArrayList<>());
		return user;
	}
}
