package com.Restaurant.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.Restaurant.Exception.CartException;
import com.Restaurant.Exception.OrderException;
import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.Exception.UserException;
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.model.Address;
import com.Restaurant.model.Notification;
import com.Restaurant.model.Order;
import com.Restaurant.model.OrderItem;
import com.Restaurant.model.Payment;
import com.Restaurant.model.PaymentResponse;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.AddressRepository;
import com.Restaurant.repository.OrderItemRepository;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.PaymentRepository;
import com.Restaurant.repository.RestaurantRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.request.CreateOrderRequest;
import com.Restaurant.request.CreateOrderItemRequest;
import com.Restaurant.repository.foodRepository;
@Service
public class OrderServiceImplementation implements OrderService {
	
	@Autowired
	private AddressRepository addressRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private foodRepository foodRepository;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PaymentService paymentSerive;
	
	@Autowired
	private NotificationService notificationService;
	@Autowired
	private RewardsService rewardsService;
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentResponse createOrder(CreateOrderRequest request, User user)
			throws UserException, RestaurantException, CartException, OrderException, StripeException {
		if (request.getClientRequestId() == null || request.getClientRequestId().isBlank()) {
			throw new OrderException("Se requiere una clave de idempotencia para crear el pedido.");
		}

		Optional<Order> existingOrder = orderRepository.findByClientRequestId(request.getClientRequestId());
		if (existingOrder.isPresent()) {
			Order order = existingOrder.get();
			if (!order.getCustomer().getId().equals(user.getId())) {
				throw new OrderException("La clave del pedido ya está asociada a otro cliente.");
			}
			if (!matchesCheckoutRequest(order, request)) {
				throw new OrderException(
						"Ya existe un Checkout pendiente para una selección distinta de productos o dirección.");
			}
			return paymentSerive.generatePaymentLink(order);
		}

		Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
				.orElseThrow(() -> new RestaurantException(
						"Restaurant not found with id " + request.getRestaurantId()));

		Address savedAddress = addressRepository.save(request.getDeliveryAddress());
		if (!user.getAddresses().contains(savedAddress)) {
			user.getAddresses().add(savedAddress);
		}

		userRepository.save(user);

		List<OrderItem> orderItems = new ArrayList<>();
		long totalAmount = 0;
		int totalItems = 0;
		for (CreateOrderItemRequest requestedItem : request.getItems()) {
			var food = foodRepository.findById(requestedItem.getMenuItemId())
					.orElseThrow(() -> new OrderException(
							"Producto no encontrado: " + requestedItem.getMenuItemId()));
			if (!food.isAvailable() || food.getRestaurant() == null
					|| !food.getRestaurant().getId().equals(restaurant.getId())
					|| food.getPrice() == null || food.getPrice() <= 0) {
				throw new OrderException("El producto no está disponible en el restaurante seleccionado.");
			}

			long lineTotal = Math.multiplyExact(food.getPrice(), requestedItem.getQuantity());
			totalAmount = Math.addExact(totalAmount, lineTotal);
			totalItems = Math.addExact(totalItems, requestedItem.getQuantity());

			OrderItem orderItem = new OrderItem();
			orderItem.setFood(food);
			orderItem.setQuantity(requestedItem.getQuantity());
			orderItem.setNotes(requestedItem.getNotes());
			orderItem.setTotalPrice(lineTotal);
			orderItems.add(orderItemRepository.save(orderItem));
		}
		if (totalAmount <= 0) {
			throw new OrderException("El total del pedido debe ser mayor que cero.");
		}

		Order createdOrder = new Order();
		createdOrder.setCustomer(user);
		createdOrder.setDeliveryAddress(savedAddress);
		createdOrder.setCreatedAt(new Date());
		createdOrder.setOrderStatus("PENDING_PAYMENT");
		createdOrder.setClientRequestId(request.getClientRequestId());
		createdOrder.setRestaurant(restaurant);
		createdOrder.setSubtotalAmount(totalAmount);
		createdOrder.setDiscountAmount(0L);
		createdOrder.setTotalAmount(totalAmount);
		createdOrder.setTotalItem(totalItems);
		createdOrder.setItems(orderItems);
		Order savedOrder = orderRepository.save(createdOrder);

		if (request.getRewardId() != null) {
			var reward = rewardsService.reserveReward(user, request.getRewardId(), savedOrder, totalAmount);
			savedOrder.setAppliedReward(reward);
			savedOrder.setRewardPointsReserved(reward.getRequiredPoints());
			savedOrder.setDiscountAmount(reward.getDiscountAmount());
			savedOrder.setTotalAmount(Math.subtractExact(totalAmount, reward.getDiscountAmount()));
			savedOrder = orderRepository.save(savedOrder);
		}

		Payment payment = new Payment();
		payment.setOrderId(savedOrder.getId());
		payment.setPaymentMethod("STRIPE_CHECKOUT");
		payment.setPaymentStatus("PENDING");
		payment.setTotalAmount(savedOrder.getTotalAmount());
		payment.setCreatedAt(new Date());
		savedOrder.setPayment(paymentRepository.save(payment));
		savedOrder = orderRepository.save(savedOrder);

		return paymentSerive.generatePaymentLink(savedOrder);
	}

	private boolean matchesCheckoutRequest(Order order, CreateOrderRequest request) {
		if (!Objects.equals(order.getRestaurant().getId(), request.getRestaurantId())
				|| !Objects.equals(order.getAppliedReward() == null ? null : order.getAppliedReward().getId(),
						request.getRewardId())
				|| order.getItems().size() != request.getItems().size()
				|| !sameAddress(order.getDeliveryAddress(), request.getDeliveryAddress())) {
			return false;
		}
		return request.getItems().stream().allMatch(requested -> order.getItems().stream().anyMatch(item ->
				Objects.equals(item.getFood().getId(), requested.getMenuItemId())
						&& item.getQuantity() == requested.getQuantity()
						&& Objects.equals(item.getNotes(), requested.getNotes())));
	}

	private boolean sameAddress(Address saved, Address requested) {
		return Objects.equals(saved.getFullName(), requested.getFullName())
				&& Objects.equals(saved.getStreetAddress(), requested.getStreetAddress())
				&& Objects.equals(saved.getCity(), requested.getCity())
				&& Objects.equals(saved.getState(), requested.getState())
				&& Objects.equals(saved.getPostalCode(), requested.getPostalCode())
				&& Objects.equals(saved.getCountry(), requested.getCountry())
				&& Objects.equals(saved.getNotes(), requested.getNotes());
	}

	@Override
	public PaymentResponse getPaymentStatus(Long orderId, User user) throws OrderException, StripeException {
		Order order = findOrderById(orderId);
		if (order.getCustomer() == null || !order.getCustomer().getId().equals(user.getId())) {
			throw new OrderException("No tienes permiso para consultar este pedido.");
		}
		return paymentSerive.refreshPaymentStatus(order);
	}

	@Override
	public void cancelOrder(Long orderId) throws OrderException {
           Order order =findOrderById(orderId);
           if(order==null) {
        	   throw new OrderException("Order not found with the id "+orderId);
           }
		
		    orderRepository.deleteById(orderId);
		
	}
	
	public Order findOrderById(Long orderId) throws OrderException {
		Optional<Order> order = orderRepository.findById(orderId);
		if(order.isPresent()) return order.get();
		
		throw new OrderException("Order not found with the id "+orderId);
	}

	@Override
	public List<Order> getUserOrders(Long userId) throws OrderException {
		List<Order> orders=orderRepository.findAllUserOrders(userId);
		return orders;
	} 

	@Override
	public List<Order> getOrdersOfRestaurant(Long restaurantId,String orderStatus) throws OrderException, RestaurantException {
		
			List<Order> orders = orderRepository.findOrdersByRestaurantId(restaurantId);
			orders = orders.stream()
					.filter(order -> "PAID".equals(order.getPaymentStatus()))
					.collect(Collectors.toList());
			
			if(orderStatus!=null) {
				orders = orders.stream()
						.filter(order->order.getOrderStatus().equals(orderStatus))
						.collect(Collectors.toList());
			}
			
			return orders;
	}
//    private List<MenuItem> filterByVegetarian(List<MenuItem> menuItems, boolean isVegetarian) {
//    return menuItems.stream()
//            .filter(menuItem -> menuItem.isVegetarian() == isVegetarian)
//            .collect(Collectors.toList());
//}
	
	

	@Override
	public Order updateOrder(Long orderId, String orderStatus) throws OrderException {
		Order order = findOrderById(orderId);
		if (!"PAID".equals(order.getPaymentStatus())) {
			throw new OrderException("El restaurante solo puede actualizar pedidos pagados.");
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new OrderException("Se requiere autenticación para actualizar el pedido.");
		}
		User actor = userRepository.findByEmail(authentication.getName());
		return updateOrder(orderId, orderStatus, actor);
	}

	@Override
	public Order updateOrder(Long orderId, String orderStatus, User actor) throws OrderException {
		Order order = findOrderById(orderId);
		if (!"PAID".equals(order.getPaymentStatus())) {
			throw new OrderException("El restaurante solo puede actualizar pedidos pagados.");
		}
		if (actor == null || actor.getId() == null) {
			throw new OrderException("No se pudo identificar al usuario que actualiza el pedido.");
		}
		if (actor.getRole() != USER_ROLE.ROLE_ADMIN
				&& (actor.getRole() != USER_ROLE.ROLE_RESTAURANT_OWNER
						|| order.getRestaurant() == null
						|| order.getRestaurant().getOwner() == null
						|| !actor.getId().equals(order.getRestaurant().getOwner().getId()))) {
			throw new OrderException("No tienes permiso para actualizar pedidos de este restaurante.");
		}
		
		if(orderStatus.equals("PREPARING") || orderStatus.equals("READY")
				|| orderStatus.equals("OUT_FOR_DELIVERY") || orderStatus.equals("DELIVERED")
				|| orderStatus.equals("COMPLETED") || orderStatus.equals("PENDING")) {
			String previousStatus = order.getOrderStatus();
			order.setOrderStatus(orderStatus);
			Order savedOrder = orderRepository.save(order);
			if (!orderStatus.equals(previousStatus)) {
				notificationService.sendOrderStatusNotification(savedOrder);
			}
			return savedOrder;
		}
		else throw new OrderException("Please Select A Valid Order Status");
		
		
	}
	
	

}
